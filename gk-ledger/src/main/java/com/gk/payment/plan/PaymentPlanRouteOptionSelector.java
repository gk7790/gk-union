package com.gk.payment.plan;

import com.gk.payment.entity.PaymentPlanRouteOptionEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 支付路由候选选择器�? * <p>
 * 先过滤不可用候选，再按 priority 选择最优分组；同一 priority 下按 weight 做确定性分流�? */
public final class PaymentPlanRouteOptionSelector {
    private PaymentPlanRouteOptionSelector() {
    }

    public static PaymentPlanRouteOptionEntity select(List<PaymentPlanRouteOptionEntity> options,
                                                      String seed,
                                                      Set<Long> disabledPspIds,
                                                      Set<Long> disabledAccountIds,
                                                      Predicate<PaymentPlanRouteOptionEntity> availablePredicate) {
        List<PaymentPlanRouteOptionEntity> availableOptions = available(options, disabledPspIds, disabledAccountIds, availablePredicate);
        if (availableOptions.isEmpty()) {
            throw new IllegalArgumentException("Payment plan route option is not available");
        }

        int bestPriority = availableOptions.stream()
                .map(PaymentPlanRouteOptionSelector::priority)
                .min(Integer::compareTo)
                .orElse(100);
        List<PaymentPlanRouteOptionEntity> samePriorityOptions = availableOptions.stream()
                .filter(option -> priority(option) == bestPriority)
                .sorted(routeOrder())
                .toList();
        return weightedPick(samePriorityOptions, seed);
    }

    private static List<PaymentPlanRouteOptionEntity> available(List<PaymentPlanRouteOptionEntity> options,
                                                                Set<Long> disabledPspIds,
                                                                Set<Long> disabledAccountIds,
                                                                Predicate<PaymentPlanRouteOptionEntity> availablePredicate) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return options.stream()
                .filter(option -> option != null && PaymentPlanRouteOptionStatus.ACTIVE.equals(option.getStatus()))
                .filter(option -> disabledPspIds == null || !disabledPspIds.contains(option.getPspId()))
                .filter(option -> disabledAccountIds == null || !disabledAccountIds.contains(option.getPspAccountId()))
                .filter(option -> availablePredicate == null || availablePredicate.test(option))
                .toList();
    }

    private static PaymentPlanRouteOptionEntity weightedPick(List<PaymentPlanRouteOptionEntity> options, String seed) {
        int totalWeight = options.stream()
                .mapToInt(PaymentPlanRouteOptionSelector::positiveWeight)
                .sum();
        if (totalWeight <= 0) {
            return options.get(0);
        }

        int slot = Math.floorMod(seed == null ? 0 : seed.hashCode(), totalWeight);
        int cursor = 0;
        for (PaymentPlanRouteOptionEntity option : options) {
            cursor += positiveWeight(option);
            if (slot < cursor) {
                return option;
            }
        }
        return options.get(0);
    }

    private static Comparator<PaymentPlanRouteOptionEntity> routeOrder() {
        return Comparator.comparingInt(PaymentPlanRouteOptionSelector::fallbackOrder)
                .thenComparingInt(PaymentPlanRouteOptionSelector::sort)
                .thenComparing(option -> option.getId() == null ? Long.MAX_VALUE : option.getId());
    }

    private static int priority(PaymentPlanRouteOptionEntity option) {
        return option.getPriority() == null ? 100 : option.getPriority();
    }

    private static int fallbackOrder(PaymentPlanRouteOptionEntity option) {
        return option.getFallbackOrder() == null ? 100 : option.getFallbackOrder();
    }

    private static int sort(PaymentPlanRouteOptionEntity option) {
        return option.getSort() == null ? 0 : option.getSort();
    }

    private static int positiveWeight(PaymentPlanRouteOptionEntity option) {
        return option.getWeight() == null || option.getWeight() < 0 ? 0 : option.getWeight();
    }
}
