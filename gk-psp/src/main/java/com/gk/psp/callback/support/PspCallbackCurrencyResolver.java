package com.gk.psp.callback.support;

import com.gk.psp.callback.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.enums.PspCallbackCurrencyPolicy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * PSP 回调币种解析器
 * <p>
 * 优先使用 PSP 回传currency；缺失时按适配器策略决定是否回退订单币种
 * 解析结果会写{@link PspCallbackResult#setCurrency(String)}，供后续校验、入账与通知复用
 */
@Component
@RequiredArgsConstructor
public class PspCallbackCurrencyResolver {
    private final List<PspCallbackAdapter> adapters;

    /**
     * PSP 适配器声明的策略解析币种
     */
    public void resolve(PspCallbackResult result, PspCallbackOrder order) {
        PspCallbackAdapter adapter = findAdapter(resolvePspCode(result, order));
        PspCallbackCurrencyPolicy policy = adapter == null
                ? PspCallbackCurrencyPolicy.ORDER_FALLBACK
                : adapter.currencyPolicy();
        resolve(result, order, policy);
    }

    /**
     * 按指定策略解析币种
     *
     * @throws IllegalStateException PSP/订单均无法得到可信币种，PSP 币种与订单不一
     */
    public void resolve(PspCallbackResult result, PspCallbackOrder order, PspCallbackCurrencyPolicy policy) {
        if (result == null || order == null) {
            throw new IllegalStateException("Invalid PSP callback context");
        }
        String pspCurrency = normalize(result.getCurrency());
        String orderCurrency = normalize(order.currency());

        if (StringUtils.isNotBlank(pspCurrency)) {
            assertMatch(pspCurrency, orderCurrency);
            result.setCurrency(pspCurrency);
            return;
        }

        if (policy == PspCallbackCurrencyPolicy.REQUIRE_PSP) {
            throw new IllegalStateException("PSP callback currency missing");
        }
        if (StringUtils.isBlank(orderCurrency)) {
            throw new IllegalStateException("PSP callback currency missing");
        }
        result.setCurrency(orderCurrency);
    }

    private void assertMatch(String pspCurrency, String orderCurrency) {
        if (StringUtils.isBlank(orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
        if (!StringUtils.equalsIgnoreCase(pspCurrency, orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
    }

    private String resolvePspCode(PspCallbackResult result, PspCallbackOrder order) {
        return StringUtils.defaultIfBlank(result.getPspCode(), order.pspCode());
    }

    private PspCallbackAdapter findAdapter(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return null;
        }
        return adapters.stream()
                .filter(adapter -> adapter.supports(pspCode))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String currency) {
        return StringUtils.defaultString(currency).trim().toUpperCase(Locale.ROOT);
    }
}
