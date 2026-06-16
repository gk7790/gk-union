package com.gk.infra.telegram;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final JdbcTemplate jdbcTemplate;

    @PostMapping
    @Operation(summary = "生成Telegram绑定码")
    @PreAuthorize("isAuthenticated()")
    public R<?> generate(@RequestParam(defaultValue = "USER") String purpose,
                         @RequestParam(required = false) Long tenantId,
                         @RequestParam(required = false) Long merchantId) {
        TgBindPurpose bindPurpose = TgBindPurpose.parse(purpose);
        String subjectType = ReqContextHolder.getSubjectType();
        if (subjectType == null || SubjectTypeEnum.fromCode(subjectType) == null) {
            throw new GkException("当前登录主体类型无效");
        }

        DynMap target = resolveTarget(bindPurpose, subjectType, tenantId, merchantId);
        TgBindTicket ticket = tgBindTicketService.generate(bindPurpose, target.getStr("subjectType"),
                target.getLong("tenantId", null), target.getLong("merchantId", null),
                target.getLong("subjectId", null), target.getLong("userId", null));
        DynMap result = new DynMap();
        result.put("code", ticket.getCode());
        result.put("purpose", ticket.getPurpose());
        result.put("subjectType", ticket.getSubjectType());
        result.put("tenantId", ticket.getTenantId());
        result.put("merchantId", ticket.getMerchantId());
        return R.ok(result);
    }

    private DynMap resolveTarget(TgBindPurpose purpose, String subjectType, Long tenantId, Long merchantId) {
        DynMap target;
        if (SubjectTypeEnum.PLATFORM.matches(subjectType)) {
            target = resolvePlatformTarget(tenantId, merchantId);
        } else if (SubjectTypeEnum.TENANT.matches(subjectType)) {
            target = resolveTenantTarget(tenantId, merchantId);
        } else {
            target = resolveMerchantTarget(tenantId, merchantId);
        }
        if (TgBindPurpose.MERCHANT.equals(purpose) && !SubjectTypeEnum.MERCHANT.matches(target.getStr("subjectType"))) {
            throw new GkException("商户绑定码必须指定商户主体");
        }
        return target;
    }

    private DynMap resolvePlatformTarget(Long tenantId, Long merchantId) {
        if (merchantId != null) {
            if (tenantId == null) {
                throw new GkException("平台用户生成商户绑定码时必须传tenantId");
            }
            return requireSubject(SubjectTypeEnum.MERCHANT.code(), tenantId, merchantId);
        }
        if (tenantId != null) {
            return requireSubject(SubjectTypeEnum.TENANT.code(), tenantId, null);
        }
        return currentTarget();
    }

    private DynMap resolveTenantTarget(Long tenantId, Long merchantId) {
        Long currentTenantId = ReqContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(currentTenantId)) {
            throw new GkException("租户用户不能为其他租户生成绑定码");
        }
        if (merchantId != null) {
            return requireSubject(SubjectTypeEnum.MERCHANT.code(), currentTenantId, merchantId);
        }
        return currentTarget();
    }

    private DynMap resolveMerchantTarget(Long tenantId, Long merchantId) {
        Long currentTenantId = ReqContextHolder.getTenantId();
        Long currentMerchantId = ReqContextHolder.getMerchantId();
        if (tenantId != null && !tenantId.equals(currentTenantId)) {
            throw new GkException("商户用户不能为其他租户生成绑定码");
        }
        if (merchantId != null && !merchantId.equals(currentMerchantId)) {
            throw new GkException("商户用户不能为其他商户生成绑定码");
        }
        return currentTarget();
    }

    private DynMap currentTarget() {
        DynMap target = new DynMap();
        target.put("subjectType", ReqContextHolder.getSubjectType());
        target.put("tenantId", ReqContextHolder.getTenantId());
        target.put("merchantId", ReqContextHolder.getMerchantId());
        target.put("subjectId", ReqContextHolder.getSubjectId());
        target.put("userId", ReqContextHolder.getUserId());
        return target;
    }

    private DynMap requireSubject(String subjectType, Long tenantId, Long merchantId) {
        String sql = """
                SELECT id, user_id, subject_type, tenant_id, merchant_id
                FROM sys_user_subject
                WHERE subject_type = ?
                  AND tenant_id = ?
                  AND status = 1
                  AND ((? IS NULL AND merchant_id IS NULL) OR merchant_id = ?)
                ORDER BY id ASC
                LIMIT 1
                """;
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                DynMap target = new DynMap();
                target.put("subjectId", rs.getLong("id"));
                target.put("userId", rs.getLong("user_id"));
                target.put("subjectType", rs.getString("subject_type"));
                target.put("tenantId", rs.getLong("tenant_id"));
                long targetMerchantId = rs.getLong("merchant_id");
                target.put("merchantId", rs.wasNull() ? null : targetMerchantId);
                return target;
            }, subjectType, tenantId, merchantId, merchantId);
        } catch (EmptyResultDataAccessException ex) {
            throw new GkException("目标绑定主体不存在或已禁用");
        }
    }
}
