package com.gk.telegram.command.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.utils.BizKeyUtils;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * /merchant 绑定商户 Telegram 通知: /merchant &lt;绑定码&gt;
 * <p>绑定码 = 商户 ID 的 Base32 短码，可在商户详情查看。</p>
 */
@Component
@RequiredArgsConstructor
public class BindMerchantCommandHandler implements TgCommandHandler {
    private final MerchantDao merchantDao;

    @Override
    public String command() {
        return "/merchant";
    }

    @Override
    public String description() {
        return "绑定商户通知: /merchant <绑定码>";
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
        if (ctx.getTgUserId() == null || ctx.getTgUserId() <= 0) {
            return "Telegram 用户信息无效。";
        }

        TgBotEntity bot = ctx.getBot();
        try {
            MerchantEntity merchant = bind(code.trim(), ctx.getTgUserId(),
                    bot == null ? null : bot.getTenantId(),
                    bot == null ? null : bot.getOwnerScope());
            return TgHtml.bold("绑定成功") + "\n"
                    + "商户号: " + TgHtml.code(merchant.getMerchantNo()) + "\n"
                    + "商户名: " + TgHtml.escape(merchant.getMerchantName()) + "\n"
                    + "Telegram ID: " + TgHtml.code(ctx.getTgUserId()) + "\n"
                    + "后续平台通知将发送到此 Telegram。";
        } catch (GkException ex) {
            return TgHtml.escape(ex.getMsg());
        } catch (IllegalArgumentException ex) {
            return "绑定码格式无效。";
        }
    }

    private MerchantEntity bind(String code, Long tgUserId, Long botTenantId, String botOwnerScope) {
        long merchantId = BizKeyUtils.decodeId(code);
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
        validateBotScope(merchant, botTenantId, botOwnerScope);

        if (merchant.getTgUserId() != null && !merchant.getTgUserId().equals(tgUserId)) {
            throw new GkException("该商户已绑定其他 Telegram 账号，请联系管理员解绑");
        }

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
        return merchant;
    }

    private void validateBotScope(MerchantEntity merchant, Long botTenantId, String botOwnerScope) {
        if (!SubjectTypeEnum.TENANT.matches(botOwnerScope)) {
            return;
        }
        if (botTenantId == null || !botTenantId.equals(merchant.getTenantId())) {
            throw new GkException("该商户不属于当前机器人租户");
        }
    }

    private String usage() {
        return "用法: " + TgHtml.code("/merchant <绑定码>")
                + "\n绑定码为商户 ID 的 Base32 短码，在商户后台详情可查看。";
    }
}
