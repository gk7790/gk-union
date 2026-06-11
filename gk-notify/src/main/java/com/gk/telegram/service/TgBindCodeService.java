package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.telegram.dto.TgBindCodeDTO;
import com.gk.telegram.entity.TgBindCodeEntity;

/**
 * Telegram绑定验证码服务
 */
public interface TgBindCodeService extends CrudService<TgBindCodeEntity, TgBindCodeDTO> {
    /**
     * 为当前用户生成一个一次性绑定码
     *
     * @param tenantId  租户ID
     * @param userId    系统用户ID
     * @param subjectId 主体ID(可空)
     * @return 生成的绑定码
     */
    TgBindCodeEntity generate(Long tenantId, Long userId, Long subjectId);

    /**
     * 校验并占用一个一次性绑定码(原子标记为已使用)
     *
     * @param code     绑定码
     * @param tgUserId 使用该码的TG用户ID
     * @return 校验通过的绑定码; 不存在/已用/过期返回null
     */
    TgBindCodeEntity consume(String code, Long tgUserId);
}
