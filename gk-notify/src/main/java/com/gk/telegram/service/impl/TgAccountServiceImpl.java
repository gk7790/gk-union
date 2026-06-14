package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.telegram.dao.TgAccountDao;
import com.gk.telegram.dto.TgAccountDTO;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.service.TgAccountService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TgAccountServiceImpl extends CrudServiceImpl<TgAccountDao, TgAccountEntity, TgAccountDTO> implements TgAccountService {

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

    @Override
    public TgAccountEntity getActiveBinding(Long botId, Long tgUserId) {
        return baseDao.selectOne(new QueryWrapper<TgAccountEntity>()
                .eq("bot_id", botId)
                .eq("tg_user_id", tgUserId)
                .eq("status", 1)
                .last("limit 1"));
    }

    @Override
    public void unbind(Long id) {
        baseDao.update(null, new UpdateWrapper<TgAccountEntity>()
                .eq("id", id)
                .set("status", 0)
                .set("updated_at", Instant.now()));
    }
}
