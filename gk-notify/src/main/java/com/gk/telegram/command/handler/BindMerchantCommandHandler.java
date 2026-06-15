package com.gk.telegram.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.infra.telegram.TgBindPurpose;
import com.gk.infra.telegram.TgBindTicket;
import com.gk.infra.telegram.TgBindTicketService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
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

import java.util.Objects;

/**
 * /merchant 指令处理器。
 *
 * <p>只消费 MERCHANT_NOTIFY 用途的绑定票据，绑定或更新商户主通知 Telegram 账号。</p>
 */
@Component
@RequiredArgsConstructor
public class BindMerchantCommandHandler implements TgCommandHandler {
    private final MerchantDao merchantDao;
    private final TgBindTicketService tgBindTicketService;
    private final SysUserSubjectService sysUserSubjectService;
    private final TgAccountService tgAccountService;

    @Override
    public String command() {
        return "/merchant";
    }

    @Override
    public String description() {
        return "绑定商户主通知账号: /merchant <绑定码>";
    }

    @Override
    public boolean requireBinding() {
        return false;
    }

    @Override
    public String handle(TgCommandContext ctx) {
        String code = ctx.arg(0);
        if (code == null || code.isBlank()) {
            return usage();
        }
        if (!ctx.isPrivateChat()) {
            return "请在机器人私聊中绑定商户账号。";
        }
        try {
            BindingTarget target = bindMerchant(ctx, code.trim());
            return TgHtml.bold("商户绑定成功") + "\n"
                    + "商户号: " + TgHtml.code(target.merchant().getMerchantNo()) + "\n"
                    + "商户名: " + TgHtml.escape(target.merchant().getMerchantName()) + "\n"
                    + "Telegram ID: " + TgHtml.code(ctx.getTgUserId());
        } catch (GkException ex) {
            return TgHtml.escape(ex.getMsg());
        }
    }

    private BindingTarget bindMerchant(TgCommandContext ctx, String code) {
        BindingTarget target = consumeTarget(ctx, code);
        MerchantEntity merchant = target.merchant();
        Long tgUserId = ctx.getTgUserId();
        MerchantEntity occupied = merchantDao.selectOne(new QueryWrapper<MerchantEntity>()
                .eq("tg_user_id", tgUserId)
                .ne("id", merchant.getId())
                .last("limit 1"));
        if (occupied != null) {
            throw new GkException("当前 Telegram 已绑定商户 " + occupied.getMerchantNo());
        }
        if (merchant.getTgUserId() == null || !merchant.getTgUserId().equals(tgUserId)) {
            MerchantEntity update = new MerchantEntity();
            update.setId(merchant.getId());
            update.setTgUserId(tgUserId);
            merchantDao.updateById(update);
            merchant.setTgUserId(tgUserId);
        }
        tgAccountService.bindSubjectAccount(ctx.getBot().getId(), tgUserId,
                ctx.getTgUsername(), ctx.getLanguageCode(), target.subject());
        return target;
    }

    private BindingTarget consumeTarget(TgCommandContext ctx, String code) {
        validateTelegramContext(ctx);
        TgBindTicket ticket = tgBindTicketService.consume(code, TgBindPurpose.MERCHANT_NOTIFY);
        if (ticket == null || ticket.getSubjectId() == null) {
            throw new GkException("绑定码无效、用途不匹配或已过期，请重新生成");
        }
        SysUserSubjectEntity subject = sysUserSubjectService.selectById(ticket.getSubjectId());
        validateTicket(ticket, subject);
        validateSubject(subject, ctx.getBot());
        MerchantEntity merchant = merchantDao.selectById(subject.getMerchantId());
        validateMerchant(subject, merchant);
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
                || !Objects.equals(ticket.getUserId(), subject.getUserId())) {
            throw new GkException("绑定码与商户主体不匹配，请重新生成");
        }
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

    private String usage() {
        return "用法: " + TgHtml.code("/merchant <绑定码>");
    }

    private record BindingTarget(SysUserSubjectEntity subject, MerchantEntity merchant) {
    }
}
