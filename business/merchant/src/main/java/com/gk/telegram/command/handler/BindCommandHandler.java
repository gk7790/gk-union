package com.gk.telegram.command.handler;

import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.telegram.bind.TgBindPurpose;
import com.gk.telegram.bind.TgBindTicket;
import com.gk.telegram.bind.TgBindTicketService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysUserSubjectService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.service.TgAccountService;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * /bind 指令处理器。
 *
 * <p>只消费 USER 用途的绑定票据，把当前 Telegram 用户绑定到票据对应的系统主体。</p>
 */
@Component
@RequiredArgsConstructor
public class BindCommandHandler implements TgCommandHandler {
    private final MerchantDao merchantDao;
    private final TgBindTicketService tgBindTicketService;
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
            return successMessage(target);
        } catch (GkException ex) {
            return TgHtml.escape(ex.getMsg());
        }
    }

    private BindingTarget consumeTarget(TgCommandContext ctx, String code) {
        validateTelegramContext(ctx);
        TgBindTicket ticket = tgBindTicketService.consume(code, TgBindPurpose.USER);
        if (ticket == null || ticket.getSubjectId() == null) {
            throw new GkException("绑定码无效、用途不匹配或已过期，请重新生成");
        }
        SysUserSubjectEntity subject = sysUserSubjectService.selectById(ticket.getSubjectId());
        validateTicket(ticket, subject);
        validateSubject(subject, ctx.getBot());
        MerchantEntity merchant = null;
        if (SubjectTypeEnum.MERCHANT.matches(subject.getSubjectType())) {
            merchant = merchantDao.selectById(subject.getMerchantId());
            validateMerchant(subject, merchant);
        }
        return new BindingTarget(subject, merchant);
    }

    private void validateTelegramContext(TgCommandContext ctx) {
        if (ctx.getBot() == null || ctx.getBot().getId() == null) {
            throw new GkException("Telegram 机器人信息无效");
        }
        if (ctx.getTgUserId() == null || ctx.getTgUserId() <= 0) {
            throw new GkException("Telegram 用户信息无效");
        }
    }

    private void validateTicket(TgBindTicket ticket, SysUserSubjectEntity subject) {
        if (subject == null) {
            return;
        }
        if (!Objects.equals(ticket.getTenantId(), subject.getTenantId())
                || !Objects.equals(ticket.getMerchantId(), subject.getMerchantId())
                || !Objects.equals(ticket.getUserId(), subject.getUserId())
                || !Objects.equals(ticket.getSubjectType(), subject.getSubjectType())) {
            throw new GkException("绑定码与系统主体不匹配，请重新生成");
        }
    }

    private void validateSubject(SysUserSubjectEntity subject, TgBotEntity bot) {
        if (subject == null) {
            throw new GkException("绑定主体不存在");
        }
        if (!Integer.valueOf(1).equals(subject.getStatus())) {
            throw new GkException("绑定主体已禁用");
        }
        SubjectTypeEnum subjectType = SubjectTypeEnum.fromCode(subject.getSubjectType());
        if (subjectType == null) {
            throw new GkException("绑定主体类型无效");
        }
        if (subject.getUserId() == null) {
            throw new GkException("绑定主体用户信息不完整，无法绑定");
        }
        if (SubjectTypeEnum.TENANT.matches(subject.getSubjectType()) && subject.getTenantId() == null) {
            throw new GkException("租户主体信息不完整，无法绑定");
        }
        if (SubjectTypeEnum.MERCHANT.matches(subject.getSubjectType())
                && (subject.getTenantId() == null || subject.getMerchantId() == null)) {
            throw new GkException("商户主体信息不完整，无法绑定");
        }
        if (bot != null && SubjectTypeEnum.TENANT.matches(bot.getOwnerScope())
                && !Objects.equals(subject.getTenantId(), bot.getTenantId())) {
            throw new GkException("绑定主体不属于当前机器人租户");
        }
    }

    private String successMessage(BindingTarget target) {
        StringBuilder sb = new StringBuilder(TgHtml.bold("账号绑定成功"));
        sb.append("\n主体类型: ").append(TgHtml.code(target.subject().getSubjectType()));
        if (target.merchant() != null) {
            sb.append("\n商户号: ").append(TgHtml.code(target.merchant().getMerchantNo()));
            sb.append("\n商户名: ").append(TgHtml.escape(target.merchant().getMerchantName()));
        }
        return sb.toString();
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
