package com.gk.telegram.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.utils.BizKeyUtils;
import com.gk.telegram.config.TgProperties;
import com.gk.telegram.dao.TgBindCodeDao;
import com.gk.telegram.dto.TgBindCodeDTO;
import com.gk.telegram.entity.TgBindCodeEntity;
import com.gk.telegram.service.TgBindCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TgBindCodeServiceImpl extends CrudServiceImpl<TgBindCodeDao, TgBindCodeEntity, TgBindCodeDTO> implements TgBindCodeService {
    private final TgProperties properties;

    @Override
    public QueryWrapper<TgBindCodeEntity> getWrapper(DynMap params) {
        QueryWrapper<TgBindCodeEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long userId = params.getLong("userId", null);
        String code = params.getStr("code");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(userId != null, "user_id", userId);
        wrapper.eq(StrUtil.isNotBlank(code), "code", code);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    @Override
    public TgBindCodeEntity generate(Long tenantId, Long userId, Long subjectId) {
        Instant now = Instant.now();
        TgBindCodeEntity entity = new TgBindCodeEntity();
        entity.setTenantId(tenantId);
        entity.setCode(BizKeyUtils.genShortCode());
        entity.setUserId(userId);
        entity.setSubjectId(subjectId);
        entity.setStatus(0);
        entity.setExpireAt(now.plus(properties.getBindCodeTtlMinutes(), ChronoUnit.MINUTES));
        entity.setCreatedAt(now);
        baseDao.insert(entity);
        return entity;
    }

    @Override
    public TgBindCodeEntity consume(String code, Long tgUserId) {
        TgBindCodeEntity entity = baseDao.selectOne(new QueryWrapper<TgBindCodeEntity>()
                .eq("code", code)
                .last("limit 1"));
        if (entity == null) {
            return null;
        }
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            return null;
        }
        if (entity.getExpireAt() == null || entity.getExpireAt().isBefore(Instant.now())) {
            return null;
        }

        Instant now = Instant.now();
        // 乐观占用: 仅当状态仍为待使用(0)时置为已使用(1), 防并发重复绑定
        int updated = baseDao.update(null, new UpdateWrapper<TgBindCodeEntity>()
                .eq("id", entity.getId())
                .eq("status", 0)
                .set("status", 1)
                .set("tg_user_id", tgUserId)
                .set("used_at", now));
        if (updated <= 0) {
            return null;
        }
        entity.setStatus(1);
        entity.setTgUserId(tgUserId);
        entity.setUsedAt(now);
        return entity;
    }
}
