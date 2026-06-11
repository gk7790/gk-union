package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.BizKeyUtils;
import com.gk.telegram.bot.TgBotApiClient;
import com.gk.telegram.config.TgProperties;
import com.gk.telegram.dao.TgBotDao;
import com.gk.telegram.dto.TgBotDTO;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.service.TgBotService;
import com.gk.telegram.support.TgTokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TgBotServiceImpl extends CrudServiceImpl<TgBotDao, TgBotEntity, TgBotDTO> implements TgBotService {
    private static final String BOT_NO_PREFIX = "TG";

    private final TgTokenCipher tokenCipher;
    private final TgBotApiClient botApiClient;
    private final TgProperties properties;

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

    @Override
    public TgBotEntity getByBotNo(String botNo) {
        return baseDao.selectOne(new QueryWrapper<TgBotEntity>()
                .eq("bot_no", botNo)
                .last("limit 1"));
    }

    @Override
    public void save(TgBotDTO dto) {
        if (StrUtil.isBlank(dto.getToken())) {
            throw new GkException("Bot Token 不能为空");
        }
        TgBotEntity entity = new TgBotEntity();
        BeanUtils.copyProperties(dto, entity);

        entity.setId(null);
        entity.setBotNo(StrUtil.isNotBlank(dto.getBotNo()) ? dto.getBotNo() : BOT_NO_PREFIX + BizKeyUtils.genShortCode());
        entity.setTokenCipher(tokenCipher.encrypt(dto.getToken()));
        entity.setTokenHash(tokenCipher.hash(dto.getToken()));
        entity.setSecretToken(BizKeyUtils.genApiSecret());
        entity.setOwnerScope(StrUtil.isNotBlank(dto.getOwnerScope()) ? dto.getOwnerScope() : "PLATFORM");
        entity.setMode(StrUtil.isNotBlank(dto.getMode()) ? dto.getMode() : "WEBHOOK");
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        // 连通测试(getMe)回填 username/botUserId, 失败不阻断创建
        JSONObject me = botApiClient.getMe(dto.getToken());
        if (me != null) {
            entity.setBotUserId(me.getLong("id"));
            if (StrUtil.isBlank(entity.getUsername())) {
                entity.setUsername(me.getString("username"));
            }
        }
        if (StrUtil.isBlank(entity.getUsername())) {
            throw new GkException("无法获取Bot username, 请检查Token是否正确或手动填写username");
        }

        insert(entity);
        dto.setId(entity.getId());
        dto.setBotNo(entity.getBotNo());
        dto.setToken(null);
    }

    @Override
    public void update(TgBotDTO dto) {
        TgBotEntity entity = new TgBotEntity();
        BeanUtils.copyProperties(dto, entity);
        // token为空表示不修改; 非空则重新加密
        if (StrUtil.isNotBlank(dto.getToken())) {
            entity.setTokenCipher(tokenCipher.encrypt(dto.getToken()));
            entity.setTokenHash(tokenCipher.hash(dto.getToken()));
        }
        updateById(entity);
        dto.setToken(null);
    }

    @Override
    public String setupWebhook(Long id) {
        TgBotEntity bot = baseDao.selectById(id);
        if (bot == null) {
            throw new GkException("机器人不存在");
        }
        if (StrUtil.isBlank(properties.getWebhookBaseUrl())) {
            throw new GkException("未配置 telegram.webhook-base-url");
        }
        String token = tokenCipher.decrypt(bot.getTokenCipher());
        String url = properties.getWebhookBaseUrl().replaceAll("/+$", "") + "/tg/webhook/" + bot.getBotNo();
        boolean ok = botApiClient.setWebhook(token, url, bot.getSecretToken());
        if (!ok) {
            throw new GkException("设置Webhook失败, 请检查网络与Token");
        }
        TgBotEntity update = new TgBotEntity();
        update.setId(id);
        update.setWebhookUrl(url);
        updateById(update);
        return url;
    }

    @Override
    public String testConnectivity(Long id) {
        TgBotEntity bot = baseDao.selectById(id);
        if (bot == null) {
            throw new GkException("机器人不存在");
        }
        String token = tokenCipher.decrypt(bot.getTokenCipher());
        JSONObject me = botApiClient.getMe(token);
        if (me == null) {
            return null;
        }
        TgBotEntity update = new TgBotEntity();
        update.setId(id);
        update.setBotUserId(me.getLong("id"));
        update.setUsername(me.getString("username"));
        updateById(update);
        return me.getString("username");
    }
}
