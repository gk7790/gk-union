package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.Result;
import com.gk.telegram.dto.TgBotDTO;
import com.gk.telegram.entity.TgBotEntity;

/**
 * Telegram机器人服务
 */
public interface TgBotService extends CrudService<TgBotEntity, TgBotDTO> {
    /**
     * 按内部机器人编号查询
     *
     * @param botNo 机器人编号
     * @return 机器人, 不存在返回null
     */
    TgBotEntity getByBotNo(String botNo);

    /**
     * 设置Telegram Webhook(拼接回调URL并下发secret_token)
     *
     * @param id 机器人ID
     * @return 已设置的回调URL
     */
    Result<String> setupWebhook(Long id);

    /**
     * 连通测试(getMe), 成功回填username/botUserId
     *
     * @param id 机器人ID
     * @return Bot @username, 失败返回null
     */
    Result<String> testConnectivity(Long id);
}
