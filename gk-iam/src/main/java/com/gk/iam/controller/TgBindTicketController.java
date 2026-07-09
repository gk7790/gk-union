package com.gk.iam.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.infra.telegram.TgBindPurpose;
import com.gk.infra.telegram.TgBindTicket;
import com.gk.infra.telegram.TgBindTicketService;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysUserSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Telegram 绑定码生成接口。
 */
@Tag(name = "Telegram绑定码")
@RestController
@RequestMapping("/tg/bind-ticket")
@RequiredArgsConstructor
public class TgBindTicketController {
    private final TgBindTicketService tgBindTicketService;
    private final SysUserSubjectService sysUserSubjectService;

    @PostMapping
    @Operation(summary = "生成绑定码")
    @PreAuthorize("isAuthenticated()")
    public R<?> generate(@RequestMap DynMap params) {
        if (params == null) {
            params = new DynMap();
        }
        String purpose = params.getStr("purpose", TgBindPurpose.CHAT.name());
        TgBindPurpose bindPurpose = TgBindPurpose.parse(purpose);
        if (!TgBindPurpose.CHAT.equals(bindPurpose)) {
            throw new GkException("当前接口暂只支持生成群绑定码");
        }

        String subjectType = ReqContextHolder.getSubjectType();
        if (subjectType == null || SubjectTypeEnum.fromCode(subjectType) == null) {
            throw new GkException("当前登录主体类型无效");
        }

        TargetRequest target = resolveChatTarget(params, subjectType);
        SysUserSubjectEntity subject = resolveSubject(target);
        TgBindTicket ticket = tgBindTicketService.generate(bindPurpose, subject.getSubjectType(),
                subject.getTenantId(), subject.getMerchantId(), subject.getId(), subject.getUserId());
        DynMap result = new DynMap();
        result.put("code", ticket.getCode());
        result.put("purpose", ticket.getPurpose());
        result.put("subjectType", ticket.getSubjectType());
        result.put("tenantId", ticket.getTenantId());
        result.put("merchantId", ticket.getMerchantId());
        result.put("subjectId", ticket.getSubjectId());
        result.put("userId", ticket.getUserId());
        return R.ok(result);
    }

    private TargetRequest resolveChatTarget(DynMap params, String currentSubjectType) {
        if (SubjectTypeEnum.PLATFORM.matches(currentSubjectType)) {
            String targetSubjectType = resolveRequestedSubjectType(params, currentSubjectType);
            return normalizeTarget(targetSubjectType,
                    params.getLong("tenantId", null),
                    params.getLong("merchantId", null),
                    resolveSubjectId(params, targetSubjectType, currentSubjectType));
        }
        if (SubjectTypeEnum.TENANT.matches(currentSubjectType)) {
            String targetSubjectType = resolveRequestedSubjectType(params, currentSubjectType);
            TargetRequest target = normalizeTarget(targetSubjectType,
                    ReqContextHolder.getTenantId(),
                    params.getLong("merchantId", null),
                    resolveSubjectId(params, targetSubjectType, currentSubjectType));
            if (SubjectTypeEnum.PLATFORM.matches(target.subjectType())) {
                throw new GkException("租户只能生成租户或商户主体绑定码");
            }
            if (SubjectTypeEnum.TENANT.matches(target.subjectType())) {
                return new TargetRequest(target.subjectType(), target.tenantId(), null, target.subjectId());
            }
            return target;
        }
        return normalizeTarget(SubjectTypeEnum.MERCHANT.code(),
                ReqContextHolder.getTenantId(),
                ReqContextHolder.getMerchantId(),
                resolveSubjectId(params, SubjectTypeEnum.MERCHANT.code(), currentSubjectType));
    }

    private String resolveRequestedSubjectType(DynMap params, String defaultSubjectType) {
        String requestedSubjectType = params.getStr("subjectType");
        if (requestedSubjectType != null && !requestedSubjectType.isBlank()) {
            return requestedSubjectType;
        }
        if (params.getLong("merchantId", null) != null) {
            return SubjectTypeEnum.MERCHANT.code();
        }
        if (params.getLong("tenantId", null) != null) {
            return SubjectTypeEnum.TENANT.code();
        }
        return defaultSubjectType;
    }

    private Long resolveSubjectId(DynMap params, String targetSubjectType, String currentSubjectType) {
        Long subjectId = params.getLong("subjectId", null);
        if (subjectId != null) {
            return subjectId;
        }
        boolean hasTargetScope = params.getLong("tenantId", null) != null || params.getLong("merchantId", null) != null;
        SubjectTypeEnum targetType = SubjectTypeEnum.fromCode(targetSubjectType);
        if (!hasTargetScope && targetType != null && targetType.matches(currentSubjectType)) {
            return ReqContextHolder.getSubjectId();
        }
        return null;
    }

    private TargetRequest normalizeTarget(String subjectType, Long tenantId, Long merchantId, Long subjectId) {
        SubjectTypeEnum type = SubjectTypeEnum.fromCode(subjectType);
        if (type == null) {
            throw new GkException("绑定主体类型无效");
        }
        if (SubjectTypeEnum.PLATFORM.matches(type.code())) {
            if (tenantId != null || merchantId != null) {
                throw new GkException("平台主体绑定码不能指定租户或商户");
            }
            return new TargetRequest(type.code(), null, null, subjectId);
        }
        if (SubjectTypeEnum.TENANT.matches(type.code())) {
            if (merchantId != null) {
                throw new GkException("租户主体绑定码不能指定商户");
            }
            if (tenantId == null) {
                throw new GkException("租户主体绑定码必须指定租户");
            }
            return new TargetRequest(type.code(), tenantId, null, subjectId);
        }
        if (tenantId == null || merchantId == null) {
            throw new GkException("商户主体绑定码必须指定租户和商户");
        }
        return new TargetRequest(type.code(), tenantId, merchantId, subjectId);
    }

    private SysUserSubjectEntity resolveSubject(TargetRequest target) {
        SysUserSubjectEntity subject = sysUserSubjectService.getActiveSubject(target.subjectType(),
                target.tenantId(), target.merchantId(), target.subjectId(), null);
        if (subject == null || subject.getId() == null) {
            throw new GkException("绑定主体不存在或已禁用");
        }
        return subject;
    }

    private record TargetRequest(String subjectType, Long tenantId, Long merchantId, Long subjectId) {
    }
}
