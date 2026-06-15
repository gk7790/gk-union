package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.Result;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.config.model.TgBaseConfig;
import com.gk.infra.config.service.SysParamsService;
import com.gk.telegram.bot.TgBotApiClient;
import com.gk.telegram.dao.TgBotDao;
import com.gk.telegram.dto.TgBotDTO;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.service.TgBotService;
import com.gk.telegram.support.TgTokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * Telegram 机器人配置服务实现。
 * <p>负责 token 加密落库、Webhook 注册、连通性检测以及 sys_params 基础配置读取。</p>
 */
@Service
@RequiredArgsConstructor
public class TgBotServiceImpl extends CrudServiceImpl<TgBotDao, TgBotEntity, TgBotDTO> implements TgBotService {
    /** 系统内部 Telegram 机器人编号前缀。 */
    private static final String BOT_NO_PREFIX = "TG";

    private final TgTokenCipher tokenCipher;
    private final TgBotApiClient botApiClient;
    private final SysParamsService sysParamsService;

    /**
     * 构造后台机器人列表的查询条件。
     */
    @Override
    public QueryWrapper<TgBotEntity> getWrapper(DynMap params) {
        QueryWrapper<TgBotEntity> wrapper = new QueryWrapper<>();
        String ownerScope = params.getStr("ownerScope");
        Long tenantId = params.getLong("tenantId", null);
        String botNo = params.getStr("botNo");
        String username = params.getStr("username");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(StrUtil.isNotBlank(ownerScope), "owner_scope", ownerScope);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(StrUtil.isNotBlank(botNo), "bot_no", botNo);
        wrapper.like(StrUtil.isNotBlank(username), "username", username);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    /**
     * 根据 webhook 路径中的 botNo 查询机器人配置。
     */
    @Override
    public TgBotEntity getByBotNo(String botNo) {
        return baseDao.selectOne(new QueryWrapper<TgBotEntity>()
                .eq("bot_no", botNo)
                .last("limit 1"));
    }

    /**
     * 新增机器人配置。
     * <p>
     * 保存前会加密 token、生成 token_hash 和 webhook secret，并调用 getMe 校验 token 可用性。
     */
    @Override
    public void save(TgBotDTO dto) {
        if (StrUtil.isBlank(dto.getToken())) {
            throw new GkException("Bot Token cannot be blank");
        }
        TgBotEntity entity = new TgBotEntity();
        BeanUtils.copyProperties(dto, entity);

        entity.setId(null);
        entity.setBotNo(StrUtil.isNotBlank(dto.getBotNo()) ? dto.getBotNo() : BOT_NO_PREFIX + BizKeyUtils.genShortCode());
        entity.setTokenCipher(tokenCipher.encrypt(dto.getToken()));
        entity.setTokenHash(tokenCipher.hash(dto.getToken()));
        entity.setSecretToken(BizKeyUtils.genApiSecret());
        entity.setOwnerScope(StrUtil.isNotBlank(dto.getOwnerScope()) ? dto.getOwnerScope() : SubjectTypeEnum.PLATFORM.code());
        entity.setMode(StrUtil.isNotBlank(dto.getMode()) ? dto.getMode() : "WEBHOOK");
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        // getMe 成功才能证明 token 可用，同时可回填 bot_user_id 和 username。
        Result<JSONObject> getMeResult = botApiClient.getMe(dto.getToken());
        JSONObject me = getMeResult.getData();
        if (getMeResult.isSuccess() && me != null) {
            entity.setBotUserId(me.getLong("id"));
            if (StrUtil.isBlank(entity.getUsername())) {
                entity.setUsername(me.getString("username"));
            }
        }
        if (StrUtil.isBlank(entity.getUsername())) {
            throw new GkException("Unable to get Bot username: " + getMeResult.getMsg());
        }

        insert(entity);
        dto.setId(entity.getId());
        dto.setBotNo(entity.getBotNo());
        dto.setToken(null);
    }

    /**
     * 修改机器人配置。
     * <p>只有传入新 token 时才重新生成密文和 hash，避免空 token 覆盖原配置。</p>
     */
    @Override
    public void update(TgBotDTO dto) {
        TgBotEntity entity = new TgBotEntity();
        BeanUtils.copyProperties(dto, entity);
        if (StrUtil.isNotBlank(dto.getToken())) {
            entity.setTokenCipher(tokenCipher.encrypt(dto.getToken()));
            entity.setTokenHash(tokenCipher.hash(dto.getToken()));
        }
        updateById(entity);
        dto.setToken(null);
    }

    /**
     * 调用 Telegram setWebhook，并把已设置的 webhookUrl 回写到 tg_bot。
     */
    @Override
    public Result<String> setupWebhook(Long id) {
        TgBotEntity bot = baseDao.selectById(id);
        if (bot == null) {
            throw new GkException("Bot does not exist");
        }

        TgBaseConfig tgBase = sysParamsService.getValueObject(Constant.TELEGRAM_BASE_CONFIG_KEY, TgBaseConfig.class);
        String webhookBaseUrl = tgBase == null ? null : tgBase.getWebhookBaseUrl();
        if (StrUtil.isBlank(webhookBaseUrl)) {
            return Result.fail("Missing TELEGRAM_BASE_CONFIG_KEY.webhookBaseUrl");
        }
        String token = tokenCipher.decrypt(bot.getTokenCipher());
        // webhookBaseUrl 由 sys_params 管理，botNo 用于路由到具体机器人配置。
        String url = webhookBaseUrl.replaceAll("/+$", "") + "/tg/webhook/" + bot.getBotNo();
        Result<JSONObject> webhookResult = botApiClient.setWebhook(token, url, bot.getSecretToken());
        if (webhookResult.isFail()) {
            return Result.fail(webhookResult.getMsg());
        }

        TgBotEntity update = new TgBotEntity();
        update.setId(id);
        update.setWebhookUrl(url);
        updateById(update);
        return Result.success(url, "Webhook set successfully");
    }

    /**
     * 使用已保存的 token 调用 getMe，验证机器人与 Telegram API 的连通性。
     */
    @Override
    public Result<String> testConnectivity(Long id) {
        TgBotEntity bot = baseDao.selectById(id);
        if (bot == null) {
            throw new GkException("Bot does not exist");
        }
        String token = tokenCipher.decrypt(bot.getTokenCipher());
        Result<JSONObject> getMeResult = botApiClient.getMe(token);
        if (getMeResult.isFail()) {
            return Result.fail(getMeResult.getMsg());
        }
        JSONObject me = getMeResult.getData();
        if (me == null) {
            return Result.fail("Telegram API result is empty");
        }

        // 连通性测试成功时顺手刷新 bot_user_id 和 username，避免 Telegram 侧信息变更后本地陈旧。
        TgBotEntity update = new TgBotEntity();
        update.setId(id);
        update.setBotUserId(me.getLong("id"));
        update.setUsername(me.getString("username"));
        updateById(update);
        return Result.success(me.getString("username"), "Telegram API connectivity test succeeded");
    }
}
