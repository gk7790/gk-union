package com.gk.payment.plan;

import com.gk.payment.fee.MerchantFeeResult;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayinPlan {
    private Long catalogId;
    private Long catalogVersion;
    private Long bucketId;
    private Long routeOptionId;
    private MerchantFeeResult merchantFee;
    private PspRouteResult route;
    private PspFeeResult pspFee;
    private BigDecimal merchantFeeAmount;
    private BigDecimal settleAmount;
    private BigDecimal pspFeeAmount;
}
