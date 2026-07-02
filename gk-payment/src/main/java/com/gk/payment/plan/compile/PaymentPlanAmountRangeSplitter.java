package com.gk.payment.plan.compile;

import com.gk.payment.plan.model.PaymentPlanAmountRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Splits the publish amount range into stable runtime buckets.
 */
public final class PaymentPlanAmountRangeSplitter {
    private static final int MONEY_SCALE = 8;
    private static final BigDecimal UNIT = new BigDecimal("0.00000001");

    private PaymentPlanAmountRangeSplitter() {
    }

    public static List<PaymentPlanAmountRange> split(BigDecimal minAmount,
                                                     BigDecimal maxAmount,
                                                     List<PaymentPlanAmountRange> sourceRanges) {
        BigDecimal start = scale(minAmount);
        BigDecimal endExclusive = scale(maxAmount).add(UNIT);
        TreeSet<BigDecimal> boundaries = new TreeSet<>();
        boundaries.add(start);
        boundaries.add(endExclusive);

        if (sourceRanges != null) {
            for (PaymentPlanAmountRange range : sourceRanges) {
                if (range == null) {
                    continue;
                }
                addBoundary(boundaries, scale(range.startAmount()), start, endExclusive);
                if (range.endAmount() != null) {
                    addBoundary(boundaries, scale(range.endAmount()).add(UNIT), start, endExclusive);
                }
            }
        }

        List<BigDecimal> points = new ArrayList<>(boundaries);
        List<PaymentPlanAmountRange> ranges = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            BigDecimal rangeStart = points.get(i);
            BigDecimal rangeEnd = points.get(i + 1);
            if (rangeStart.compareTo(rangeEnd) < 0) {
                ranges.add(new PaymentPlanAmountRange(rangeStart, rangeEnd));
            }
        }
        return ranges;
    }

    private static void addBoundary(TreeSet<BigDecimal> boundaries,
                                    BigDecimal boundary,
                                    BigDecimal start,
                                    BigDecimal endExclusive) {
        if (boundary != null && boundary.compareTo(start) > 0 && boundary.compareTo(endExclusive) < 0) {
            boundaries.add(boundary);
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
