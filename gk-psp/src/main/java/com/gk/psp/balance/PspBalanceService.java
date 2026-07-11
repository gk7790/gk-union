package com.gk.psp.balance;

import java.math.BigDecimal;

/**
 * PSP 余额快照服务。
 * <p>
 * 这里只维护 PSP 侧余额的缓存快照，用于后台展示和代付路由弱过滤；
 * 不参与商户账务、资金扣减，也不保证强一致。
 */
public interface PspBalanceService {
    /**
     * 读取 Redis 中的余额快照。
     *
     * @return 缓存不存在、读取异常或参数不完整时返回 {@code null}
     */
    PspBalanceSnap getCached(Long tenantId, Long pspAccountId);

    /**
     * 主动查询指定 PSP 账号余额并刷新 Redis 快照。
     */
    PspBalanceSnap refresh(Long pspAccountId);

    /**
     * 批量刷新所有启用中的 PSP 账号余额快照。
     *
     * @return 成功刷新并写入缓存的账号数量
     */
    int refreshAll();

    /**
     * 判断代付路由是否可以参考当前 PSP 余额快照继续使用该账号。
     * <p>
     * 这是弱过滤：缓存缺失、过期、未知或无法判断时默认放行，只有明确余额不足时才过滤。
     *
     * @param tenantId 租户 ID
     * @param pspAccountId PSP 账号 ID
     * @param currency 订单币种
     * @param amount 订单金额
     * @return {@code true} 表示路由可继续使用该账号，{@code false} 表示快照明确余额不足
     */
    boolean isPayoutBalanceAvailable(Long tenantId, Long pspAccountId, String currency, BigDecimal amount);
}
