package com.gk.telegram.service;

import com.gk.telegram.entity.TgUpdateLogEntity;

/**
 * Telegram入站更新日志服务(幂等 + 审计)
 */
public interface TgUpdateLogService {
    /**
     * 尝试登记一条入站更新(基于 bot_id + update_id 唯一约束去重)
     *
     * @param log 入站日志(初始 handle_status=0)
     * @return true=首次登记成功; false=重复(已处理过, 应跳过)
     */
    boolean tryBegin(TgUpdateLogEntity log);

    /**
     * 标记处理结果
     *
     * @param id           日志ID
     * @param handleStatus 1成功 2失败
     * @param errorMsg     错误信息(成功为null)
     */
    void markResult(Long id, int handleStatus, String errorMsg);
}
