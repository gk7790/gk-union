package com.gk.telegram.command.handler;

import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.support.MerchantTgBindCodeService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.service.TgChatService;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 绑定当前群
 */
@Component
@RequiredArgsConstructor
public class BindChatCommandHandler implements TgCommandHandler {
    private final MerchantDao merchantDao;
    private final MerchantTgBindCodeService merchantTgBindCodeService;
    private final TgChatService tgChatService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/bindchat";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "绑定当前群: /bindchat <绑定码>";
    }

    /**
     * 群绑定入口允许未绑定群调用。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 使用一次性绑定码把当前 Telegram 群写入 tg_chat。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        String code = ctx.arg(0);
        if (code == null || code.isBlank()) {
            return "用法: " + TgHtml.code("/bindchat <绑定码>");
        }
        if (!ctx.isGroupChat()) {
            return "请在群或超级群中使用该指令。";
        }
        try {
            // 群绑定同样复用商户详情生成的绑定码，绑定成功后群可按商户范围查询订单。
            MerchantEntity merchant = consumeMerchant(ctx, code.trim());
            tgChatService.bindMerchantChat(ctx.getBot().getId(), ctx.getChatId(),
                    ctx.getChatType(), ctx.getChatTitle(), ctx.getLanguageCode(), merchant);
            return TgHtml.bold("群绑定成功") + "\n"
                    + "商户号: " + TgHtml.code(merchant.getMerchantNo()) + "\n"
                    + "商户名: " + TgHtml.escape(merchant.getMerchantName()) + "\n"
                    + "群 Chat ID: " + TgHtml.code(ctx.getChatId());
        } catch (GkException ex) {
            return TgHtml.escape(ex.getMsg());
        }
    }

    /**
     * 消费绑定码并校验商户状态、风控状态和机器人租户范围。
     */
    private MerchantEntity consumeMerchant(TgCommandContext ctx, String code) {
        if (ctx.getBot() == null || ctx.getBot().getId() == null) {
            throw new GkException("Telegram 机器人信息无效");
        }
        if (ctx.getTgUserId() == null || ctx.getTgUserId() <= 0) {
            throw new GkException("Telegram 用户信息无效");
        }
        // consume 后 Redis 中的绑定码会被删除，避免同一个码被重复使用。
        Long merchantId = merchantTgBindCodeService.consume(code);
        if (merchantId == null) {
            throw new GkException("绑定码无效或已过期，请重新生成");
        }
        MerchantEntity merchant = merchantDao.selectById(merchantId);
        if (merchant == null) {
            throw new GkException("商户不存在");
        }
        if (!Integer.valueOf(1).equals(merchant.getStatus())) {
            throw new GkException("商户已禁用，无法绑定");
        }
        if (!"NORMAL".equalsIgnoreCase(StringUtils.defaultString(merchant.getRiskStatus()))) {
            throw new GkException("商户风控状态异常，无法绑定");
        }
        validateBotScope(merchant, ctx.getBot());
        return merchant;
    }

    /**
     * 租户机器人只能绑定同租户下的商户，平台机器人不限制商户租户。
     */
    private void validateBotScope(MerchantEntity merchant, TgBotEntity bot) {
        if (bot == null || !SubjectTypeEnum.TENANT.matches(bot.getOwnerScope())) {
            return;
        }
        if (bot.getTenantId() == null || !bot.getTenantId().equals(merchant.getTenantId())) {
            throw new GkException("该商户不属于当前机器人租户");
        }
    }
}
