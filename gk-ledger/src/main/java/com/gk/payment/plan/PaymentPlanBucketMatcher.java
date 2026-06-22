package com.gk.payment.plan;

import com.gk.payment.entity.PaymentPlanBucketEntity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 金额桶匹配器。
 * <p>
 * 使用半开区间 [start, end)，最后一个 bucket 的 end 允许为空表示无上限。
 */
public final class PaymentPlanBucketMatcher {
    private PaymentPlanBucketMatcher() {
    }

    public static PaymentPlanBucketEntity match(List<PaymentPlanBucketEntity> buckets, BigDecimal amount) {
        if (buckets == null || buckets.isEmpty() || amount == null) {
            throw new IllegalArgumentException("Payment plan bucket is not configured");
        }
        List<PaymentPlanBucketEntity> matched = buckets.stream()
                .sorted(Comparator.comparing(PaymentPlanBucketEntity::getBucketStartAmount))
                .filter(bucket -> contains(bucket, amount))
                .toList();
        if (matched.isEmpty()) {
            throw new IllegalArgumentException("Payment plan bucket does not match amount");
        }
        if (matched.size() > 1) {
            throw new IllegalArgumentException("Payment plan bucket is overlapped");
        }
        return matched.get(0);
    }

    public static PaymentPlanBucket matchPlanBucket(List<PaymentPlanBucket> buckets, BigDecimal amount) {
        if (buckets == null || buckets.isEmpty() || amount == null) {
            throw new IllegalArgumentException("Payment plan bucket is not configured");
        }
        List<PaymentPlanBucket> matched = buckets.stream()
                .sorted(Comparator.comparing(item -> item.getBucket().getBucketStartAmount()))
                .filter(item -> contains(item.getBucket(), amount))
                .toList();
        if (matched.isEmpty()) {
            throw new IllegalArgumentException("Payment plan bucket does not match amount");
        }
        if (matched.size() > 1) {
            throw new IllegalArgumentException("Payment plan bucket is overlapped");
        }
        return matched.get(0);
    }

    private static boolean contains(PaymentPlanBucketEntity bucket, BigDecimal amount) {
        BigDecimal start = bucket.getBucketStartAmount();
        BigDecimal end = bucket.getBucketEndAmount();
        // start <= amount && (end is null || amount < end)
        return (start == null || start.compareTo(amount) <= 0)
                && (end == null || amount.compareTo(end) < 0);
    }
}
