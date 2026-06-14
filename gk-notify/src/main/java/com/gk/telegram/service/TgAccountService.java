package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.telegram.dto.TgAccountDTO;
import com.gk.telegram.entity.TgAccountEntity;

/**
 * Telegram账号绑定服务
 */
public interface TgAccountService extends CrudService<TgAccountEntity, TgAccountDTO> {
    /**
     * 查询某机器人下某TG用户的有效绑定
     *
     * @param botId    机器人ID
     * @param tgUserId Telegram用户ID
     * @return 绑定记录, 未绑定返回null
     */
    TgAccountEntity getActiveBinding(Long botId, Long tgUserId);

    /**
     * 解绑(置为status=0)
     *
     * @param id 绑定ID
     */
    void unbind(Long id);
}
