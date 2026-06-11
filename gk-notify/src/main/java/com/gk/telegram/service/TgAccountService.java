package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.telegram.dto.TgAccountDTO;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgBindCodeEntity;
import com.gk.telegram.entity.TgBotEntity;

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
     * 绑定TG用户到系统主体(基于已校验的绑定码)
     *
     * @param bot          机器人
     * @param tgUserId     Telegram用户ID
     * @param tgUsername   Telegram用户名
     * @param languageCode TG语言
     * @param bindCode     已校验通过的绑定码
     * @return 绑定后的账号
     */
    TgAccountEntity bind(TgBotEntity bot, Long tgUserId, String tgUsername, String languageCode, TgBindCodeEntity bindCode);

    /**
     * 解绑(置为status=0)
     *
     * @param id 绑定ID
     */
    void unbind(Long id);
}
