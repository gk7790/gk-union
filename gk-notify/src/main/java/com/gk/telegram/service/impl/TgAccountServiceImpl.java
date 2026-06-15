package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.telegram.dao.TgAccountDao;
import com.gk.telegram.dto.TgAccountDTO;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.service.TgAccountService;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Telegram 个人账号绑定服务实现。
 * <p>封装 tg_account 的查询、绑定恢复/新增和解绑状态变更逻辑。</p>
 */
@Service
public class TgAccountServiceImpl extends CrudServiceImpl<TgAccountDao, TgAccountEntity, TgAccountDTO> implements TgAccountService {

    /**
     * 构造后台 Telegram 账号绑定列表查询条件。
     */
    @Override
    public QueryWrapper<TgAccountEntity> getWrapper(DynMap params) {
        QueryWrapper<TgAccountEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long botId = params.getLong("botId", null);
        Long tgUserId = params.getLong("tgUserId", null);
        Long userId = params.getLong("userId", null);
        String tgUsername = params.getStr("tgUsername");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(botId != null, "bot_id", botId);
        wrapper.eq(tgUserId != null, "tg_user_id", tgUserId);
        wrapper.eq(userId != null, "user_id", userId);
        wrapper.like(StrUtil.isNotBlank(tgUsername), "tg_username", tgUsername);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    /**
     * 查询指定 bot 下某个 Telegram 用户的有效绑定记录。
     */
    @Override
    public TgAccountEntity getActiveBinding(Long botId, Long tgUserId) {
        return baseDao.selectOne(new QueryWrapper<TgAccountEntity>()
                .eq("bot_id", botId)
                .eq("tg_user_id", tgUserId)
                .eq("status", 1)
                .last("limit 1"));
    }

    /**
     * 新增或恢复 Telegram 用户绑定。
     * <p>表上存在 bot_id + tg_user_id 唯一键，所以这里按唯一键做 upsert 语义。</p>
     */
    @Override
    public TgAccountEntity bindMerchantAccount(Long botId, Long tgUserId, String tgUsername, String languageCode, MerchantEntity merchant) {
        if (botId == null || tgUserId == null || merchant == null) {
            throw new IllegalArgumentException("botId, tgUserId and merchant are required");
        }
        Instant now = Instant.now();
        TgAccountEntity existed = baseDao.selectOne(new QueryWrapper<TgAccountEntity>()
                .eq("bot_id", botId)
                .eq("tg_user_id", tgUserId)
                .last("limit 1"));
        if (existed == null) {
            // 首次绑定时插入新记录，user_id/subject_id 预留给后续真正系统用户绑定码使用。
            TgAccountEntity entity = new TgAccountEntity();
            entity.setTenantId(merchant.getTenantId());
            entity.setBotId(botId);
            entity.setTgUserId(tgUserId);
            entity.setTgUsername(tgUsername);
            entity.setLanguageCode(languageCode);
            entity.setStatus(1);
            entity.setBoundAt(now);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            baseDao.insert(entity);
            return entity;
        }

        // 已存在记录时恢复为有效状态，并刷新 Telegram 用户名、语言和租户信息。
        TgAccountEntity update = new TgAccountEntity();
        update.setId(existed.getId());
        update.setTenantId(merchant.getTenantId());
        update.setTgUsername(tgUsername);
        update.setLanguageCode(languageCode);
        update.setStatus(1);
        update.setBoundAt(now);
        update.setUpdatedAt(now);
        baseDao.updateById(update);

        existed.setTenantId(merchant.getTenantId());
        existed.setTgUsername(tgUsername);
        existed.setLanguageCode(languageCode);
        existed.setStatus(1);
        existed.setBoundAt(now);
        existed.setUpdatedAt(now);
        return existed;
    }

    /**
     * 解绑个人账号，保留记录用于审计和后续恢复。
     */
    @Override
    public void unbind(Long id) {
        baseDao.update(null, new UpdateWrapper<TgAccountEntity>()
                .eq("id", id)
                .set("status", 0)
                .set("updated_at", Instant.now()));
    }
}
