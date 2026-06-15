package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.platform.entity.SysUserSubjectEntity;
import com.gk.telegram.dao.TgChatDao;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.service.TgChatService;
import com.gk.telegram.support.TgConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

/**
 * Telegram 群/会话绑定服务实现。
 * <p>封装 tg_chat 的后台查询、群绑定新增/恢复和解绑状态变更逻辑。</p>
 */
@Service
public class TgChatServiceImpl extends CrudServiceImpl<TgChatDao, TgChatEntity, TgChatDTO> implements TgChatService {

    /**
     * 构造后台 Telegram 会话/群绑定列表查询条件。
     */
    @Override
    public QueryWrapper<TgChatEntity> getWrapper(DynMap params) {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long botId = params.getLong("botId", null);
        Long chatId = params.getLong("chatId", null);
        String chatType = params.getStr("chatType");
        String purpose = params.getStr("purpose");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(botId != null, "bot_id", botId);
        wrapper.eq(chatId != null, "chat_id", chatId);
        wrapper.eq(StrUtil.isNotBlank(chatType), "chat_type", chatType);
        wrapper.eq(StrUtil.isNotBlank(purpose), "purpose", purpose);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    /**
     * 查询指定 bot 下某个 Telegram chat 的有效绑定记录。
     */
    @Override
    public TgChatEntity getActiveChat(Long botId, Long chatId) {
        if (botId == null || chatId == null) {
            return null;
        }
        return baseDao.selectOne(new QueryWrapper<TgChatEntity>()
                .eq("bot_id", botId)
                .eq("chat_id", chatId)
                .eq("status", 1)
                .last("limit 1"));
    }

    /**
     * 新增或恢复 Telegram 群绑定。
     * <p>表上存在 bot_id + chat_id 唯一键，所以这里按唯一键做 upsert 语义。</p>
     */
    @Override
    public TgChatEntity bindSubjectChat(Long botId, Long chatId, String chatType, String title, String languageCode,
                                        SysUserSubjectEntity subject) {
        if (botId == null || chatId == null || subject == null || subject.getId() == null) {
            throw new IllegalArgumentException("botId, chatId and subject are required");
        }
        Instant now = Instant.now();
        String normalizedType = StringUtils.defaultIfBlank(chatType, "GROUP").toUpperCase(Locale.ROOT);
        String lang = StringUtils.defaultIfBlank(languageCode, "en-US");
        TgChatEntity existed = baseDao.selectOne(new QueryWrapper<TgChatEntity>()
                .eq("bot_id", botId)
                .eq("chat_id", chatId)
                .last("limit 1"));
        if (existed == null) {
            // 首次群绑定时登记推送目标，并默认作为通知用途。
            TgChatEntity entity = new TgChatEntity();
            entity.setTenantId(subject.getTenantId());
            entity.setMerchantId(subject.getMerchantId());
            entity.setBotId(botId);
            entity.setChatId(chatId);
            entity.setChatType(normalizedType);
            entity.setTitle(title);
            entity.setPurpose(TgConstants.ChatPurpose.NOTIFY);
            entity.setLang(lang);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            baseDao.insert(entity);
            return entity;
        }

        // 群重新绑定时刷新租户、商户、标题和语言，并恢复为启用状态。
        TgChatEntity update = new TgChatEntity();
        update.setId(existed.getId());
        update.setTenantId(subject.getTenantId());
        update.setMerchantId(subject.getMerchantId());
        update.setChatType(normalizedType);
        update.setTitle(title);
        update.setLang(lang);
        update.setStatus(1);
        update.setUpdatedAt(now);
        baseDao.updateById(update);

        existed.setTenantId(subject.getTenantId());
        existed.setMerchantId(subject.getMerchantId());
        existed.setChatType(normalizedType);
        existed.setTitle(title);
        existed.setLang(lang);
        existed.setStatus(1);
        existed.setUpdatedAt(now);
        return existed;
    }

    /**
     * 解绑群会话，保留记录用于审计和后续恢复。
     */
    @Override
    public void unbind(Long id) {
        baseDao.update(null, new UpdateWrapper<TgChatEntity>()
                .eq("id", id)
                .set("status", 3)
                .set("updated_at", Instant.now()));
    }
}
