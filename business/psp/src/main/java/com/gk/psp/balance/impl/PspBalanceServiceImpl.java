package com.gk.psp.balance.impl;

import com.gk.psp.support.PspCacheKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.task.TaskExecutionRecord;
import com.gk.common.task.TaskExecutions;
import com.gk.psp.config.PspConfigService;
import com.gk.psp.adapter.PspBalanceAdapter;
import com.gk.psp.balance.model.PspBalanceAccount;
import com.gk.psp.balance.PspBalanceSnap;
import com.gk.psp.balance.PspBalanceService;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.request.PspBalanceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * PSP 余额快照服务实现。
 * <p>
 * 余额来源于 PSP API 查询结果，落 Redis 短缓存；路由只弱参考该快照，不做资金账务职责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PspBalanceServiceImpl implements PspBalanceService {
    private final PspAccountDao pspAccountDao;
    private final List<PspBalanceAdapter> adapters;
    private final RedisUtils redisUtils;
    private final PspConfigService configService;

    @Override
    public PspBalanceSnap getCached(Long tenantId, Long pspAccountId) {
        if (tenantId == null || pspAccountId == null) {
            return null;
        }
        try {
            return redisUtils.get(PspCacheKeys.balance(tenantId, pspAccountId),
                    PspBalanceSnap.class);
        } catch (Exception ex) {
            log.warn("Get PSP account balance cache failed, pspAccountId={}, err={}", pspAccountId, ex.getMessage());
            return null;
        }
    }

    @Override
    public PspBalanceSnap refresh(Long pspAccountId) {
        // 一次 join 查出账号和通道上下文，避免先查 account 再查 provider。
        PspBalanceAccount account = pspAccountDao.selectBalanceAccount(pspAccountId);
        if (account == null) {
            throw new IllegalArgumentException("PSP account not found or disabled");
        }
        long cacheSeconds = cacheSeconds();
        PspBalanceSnap snap = query(account, cacheSeconds);
        cache(snap);
        return snap;
    }

    @Override
    public int refreshAll() {
        // 定时任务只刷新启用中的账号和通道；单个账号失败不会中断整批刷新。
        List<PspBalanceAccount> accounts = pspAccountDao.selectBalanceAccounts();
        int refreshed = 0;
        for (PspBalanceAccount account : accounts) {
            long cacheSeconds = cacheSeconds();
            TaskExecutionRecord executionRecord = TaskExecutions.current().record(
                    "PSP balance account=" + account.getPspAccountNo() + ", psp=" + account.getPspCode());
            try {
                PspBalanceSnap snap = query(account, cacheSeconds);
                cache(snap);
                refreshed++;
                executionRecord.complete("Balance snapshot refreshed, status=" + snap.getStatus());
            } catch (Exception ex) {
                log.warn("Refresh PSP account balance failed, pspAccountId={}, err={}", account.getPspAccountId(), ex.getMessage());
                cache(errorSnap(account, ex, cacheSeconds));
                executionRecord.error("REFRESH", "PSP balance refresh failed", ex);
            }
        }
        return refreshed;
    }

    @Override
    public boolean isPayoutBalanceAvailable(Long tenantId, Long pspAccountId, String currency, BigDecimal amount) {
        PspBalanceSnap snap = getCached(tenantId, pspAccountId);
        // 弱过滤：无快照、快照过期或余额未知时放行，避免缓存问题直接阻断代付。
        if (snap == null || isExpired(snap) || PspBalanceSnap.STATUS_UNKNOWN.equals(snap.getStatus())) {
            return true;
        }
        // 明确低余额时才过滤该 PSP 账号。
        if (PspBalanceSnap.STATUS_LOW.equals(snap.getStatus())) {
            return false;
        }
        // 信息不足时不拦截，由后续 PSP 提交结果兜底。
        if (snap.getAvailableBalance() == null || amount == null || amount.signum() <= 0) {
            return true;
        }
        // 币种不一致时不使用余额快照做判断，避免跨币种误过滤。
        if (StringUtils.isNotBlank(snap.getCurrency())
                && StringUtils.isNotBlank(currency)
                && !snap.getCurrency().equalsIgnoreCase(currency)) {
            return true;
        }
        return snap.getAvailableBalance().compareTo(amount) >= 0;
    }

    private PspBalanceSnap query(PspBalanceAccount account, long cacheSeconds) {
        PspBalanceAdapter adapter = adapters.stream()
                .filter(item -> item.supports(account.getPspCode()))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            return unsupportedSnap(account, cacheSeconds);
        }
        PspBalanceSnap snap = adapter.queryBalance(buildRequest(account));
        if (snap == null) {
            return unsupportedSnap(account, cacheSeconds);
        }
        // adapter 只负责三方协议解析，公共账号信息和缓存过期时间在服务层统一补齐。
        fillCommon(snap, account);
        if (StringUtils.isBlank(snap.getStatus())) {
            snap.setStatus(PspBalanceSnap.STATUS_NORMAL);
        }
        if (snap.getUpdatedAt() == null) {
            snap.setUpdatedAt(Instant.now());
        }
        snap.setExpireAt(snap.getUpdatedAt().plusSeconds(cacheSeconds));
        return snap;
    }

    private PspBalanceSnap unsupportedSnap(PspBalanceAccount account, long cacheSeconds) {
        PspBalanceSnap snap = new PspBalanceSnap();
        fillCommon(snap, account);
        snap.setStatus(PspBalanceSnap.STATUS_UNKNOWN);
        snap.setSource(PspBalanceSnap.SOURCE_SYSTEM);
        snap.setUpdatedAt(Instant.now());
        snap.setExpireAt(snap.getUpdatedAt().plusSeconds(cacheSeconds));
        snap.setErrorCode("BALANCE_QUERY_UNSUPPORTED");
        snap.setErrorMessage("PSP balance query adapter is not configured");
        return snap;
    }

    private PspBalanceRequest buildRequest(PspBalanceAccount account) {
        return PspBalanceRequest.builder()
                .tenantId(account.getTenantId())
                .pspId(account.getPspId())
                .pspCode(account.getPspCode())
                .pspBaseUrl(account.getPspBaseUrl())
                .providerConfigJson(account.getProviderConfigJson())
                .pspAccountId(account.getPspAccountId())
                .pspAccountNo(account.getPspAccountNo())
                .apiKey(account.getApiKey())
                .apiSecret(account.getApiSecret())
                .accountConfigJson(account.getAccountConfigJson())
                .build();
    }

    private PspBalanceSnap errorSnap(PspBalanceAccount account, Exception ex, long cacheSeconds) {
        PspBalanceSnap snap = new PspBalanceSnap();
        fillCommon(snap, account);
        snap.setStatus(PspBalanceSnap.STATUS_UNKNOWN);
        snap.setSource(PspBalanceSnap.SOURCE_SYSTEM);
        snap.setUpdatedAt(Instant.now());
        snap.setExpireAt(snap.getUpdatedAt().plusSeconds(cacheSeconds));
        snap.setErrorCode(ex.getClass().getSimpleName());
        snap.setErrorMessage(StringUtils.left(ex.getMessage(), 512));
        return snap;
    }

    private void fillCommon(PspBalanceSnap snap, PspBalanceAccount account) {
        snap.setTenantId(account.getTenantId());
        snap.setPspId(account.getPspId());
        snap.setPspAccountId(account.getPspAccountId());
        snap.setPspAccountNo(account.getPspAccountNo());
        snap.setPspCode(account.getPspCode());
        if (StringUtils.isNotBlank(snap.getCurrency())) {
            snap.setCurrency(snap.getCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.isBlank(snap.getSource())) {
            snap.setSource(PspBalanceSnap.SOURCE_API);
        }
    }

    private void cache(PspBalanceSnap snap) {
        if (snap == null || snap.getTenantId() == null || snap.getPspAccountId() == null) {
            return;
        }
        try {
            // Redis TTL 与 snap.expireAt 对齐，后台展示过期时间和缓存失效时间保持一致。
            redisUtils.set(PspCacheKeys.balance(snap.getTenantId(), snap.getPspAccountId()),
                    snap, cacheTtlSeconds(snap));
        } catch (Exception ex) {
            log.warn("Set PSP account balance cache failed, pspAccountId={}, err={}",
                    snap.getPspAccountId(), ex.getMessage());
        }
    }

    private long cacheTtlSeconds(PspBalanceSnap snap) {
        if (snap.getUpdatedAt() != null && snap.getExpireAt() != null) {
            long ttl = snap.getExpireAt().getEpochSecond() - Instant.now().getEpochSecond();
            if (ttl > 0) {
                return ttl;
            }
        }
        return cacheSeconds();
    }

    private long cacheSeconds() {
        return configService.balance().getCacheSeconds();
    }

    private boolean isExpired(PspBalanceSnap snap) {
        return snap.getExpireAt() != null && snap.getExpireAt().isBefore(Instant.now());
    }
}
