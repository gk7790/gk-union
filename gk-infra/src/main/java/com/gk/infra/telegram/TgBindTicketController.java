package com.gk.infra.telegram;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    @Operation(summary = "生成Telegram绑定码")
    @PreAuthorize("isAuthenticated()")
    public R<?> generate(@RequestParam(defaultValue = "USER") String purpose) {
        TgBindPurpose bindPurpose = TgBindPurpose.parse(purpose);
        String subjectType = ReqContextHolder.getSubjectType();
        if (subjectType == null || SubjectTypeEnum.fromCode(subjectType) == null) {
            throw new GkException("当前登录主体类型无效");
        }
        if (TgBindPurpose.MERCHANT.equals(bindPurpose) && !SubjectTypeEnum.MERCHANT.matches(subjectType)) {
            throw new GkException("只有商户主体可以生成商户绑定码");
        }

        TgBindTicket ticket = tgBindTicketService.generate(bindPurpose, subjectType,
                ReqContextHolder.getTenantId(), ReqContextHolder.getMerchantId(),
                ReqContextHolder.getSubjectId(), ReqContextHolder.getUserId());
        DynMap result = new DynMap();
        result.put("code", ticket.getCode());
        result.put("purpose", ticket.getPurpose());
        result.put("subjectType", ticket.getSubjectType());
        return R.ok(result);
    }
}
