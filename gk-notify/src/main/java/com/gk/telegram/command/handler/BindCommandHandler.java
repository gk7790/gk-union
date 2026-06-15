package com.gk.telegram.command.handler;

import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.support.MerchantTgBindCodeService;
import com.gk.platform.entity.SysUserSubjectEntity;
import com.gk.platform.service.SysUserSubjectService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.service.TgAccountService;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * /bind 指令处理器。
 * <p>把当前 Telegram 用户绑定到系统商户主体，后续业务指令通过 subject_id 解析商户范围。</p>
 */
@Component
@RequiredArgsConstructor
public class BindCommandHandler implements TgCommandHandler {
    private final MerchantDao merchantDao;
    private final MerchantTgBindCodeService merchantTgBindCodeService;
    private final SysUserSubjectService sysUserSubjectService;
    private final TgAccountService tgAccountService;

    @Override
    public String command() {
        return "/bind";
    }

    @Override
    public String description() {
        return "绑定当前 Telegram 用户: /bind <绑定码>";
    }

    @Override
    public boolean requireBinding() {
        return false;
    }

    @Override
    public String handle(TgCommandContext ctx) {
        String code = ctx.arg(0);
        if (code == null || code.isBlank()) {
            return "用法: " + TgHtml.code("/bind <绑定码>");
        }
        if (!ctx.isPrivateChat()) {
            return "请在机器人私聊中绑定账号。";
        }
        try {
            BindingTarget target = consumeTarget(ctx, code.trim());
            tgAccountService.bindSubjectAccount(ctx.getBot().getId(), ctx.getTgUserId(),
                    ctx.getTgUsername(), ctx.getLanguageCode(), target.subject());
            return TgHtml.bold("账号绑定成功") + "\n"
                    + "商户号: " + TgHtml.code(target.merchant().getMerchantNo()) + "\n"
                    + "商户名: " + TgHtml.escape(target.merchant().getMerchantName());
        } catch (GkException ex) {
            return TgHtml.escape(ex.getMsg());
        }
    }

    private BindingTarget consumeTarget(TgCommandContext ctx, String code) {
        if (ctx.getBot() == null || ctx.getBot().getId() == null) {
            throw new GkException("Telegram 机器人信息无效");
        }
        if (ctx.getTgUserId() == null || ctx.getTgUserId() <= 0) {
            throw new GkException("Telegram 用户信息无效");
        }
        Long subjectId = merchantTgBindCodeService.consume(code);
        if (subjectId == null) {
            throw new GkException("绑定码无效或已过期，请重新生成");
        }
        SysUserSubjectEntity subject = sysUserSubjectService.selectById(subjectId);
        validateSubject(subject, ctx.getBot());
        MerchantEntity merchant = merchantDao.selectById(subject.getMerchantId());
        validateMerchant(subject, merchant);
        return new BindingTarget(subject, merchant);
    }

    private void validateSubject(SysUserSubjectEntity subject, TgBotEntity bot) {
        if (subject == null) {
            throw new GkException("绑定主体不存在");
        }
        if (!Integer.valueOf(1).equals(subject.getStatus())) {
            throw new GkException("绑定主体已禁用");
        }
        if (!SubjectTypeEnum.MERCHANT.matches(subject.getSubjectType())) {
            throw new GkException("绑定码不是商户主体，无法绑定");
        }
        if (subject.getTenantId() == null || subject.getMerchantId() == null || subject.getUserId() == null) {
            throw new GkException("商户主体信息不完整，无法绑定");
        }
        if (bot != null && SubjectTypeEnum.TENANT.matches(bot.getOwnerScope())
                && !subject.getTenantId().equals(bot.getTenantId())) {
            throw new GkException("该商户不属于当前机器人租户");
        }
    }

    private void validateMerchant(SysUserSubjectEntity subject, MerchantEntity merchant) {
        if (merchant == null) {
            throw new GkException("商户不存在");
        }
        if (!subject.getTenantId().equals(merchant.getTenantId())) {
            throw new GkException("商户主体与商户租户不一致");
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new GkException("商户已禁用，无法绑定");
        }
        if (!"NORMAL".equalsIgnoreCase(StringUtils.defaultString(merchant.getRiskStatus()))) {
            throw new GkException("商户风控状态异常，无法绑定");
        }
    }

    private record BindingTarget(SysUserSubjectEntity subject, MerchantEntity merchant) {
    }
}
