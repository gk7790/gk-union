package com.gk.openapi.dto;

import lombok.Data;

@Data
@OpenApiModel
public class BalanceResponse {
    private String accountNo;
    private String currency;
    /** 可代�?提现的可用余�?*/
    private String balance;
    /** 代收成功尚未释放的待结算余额 */
    private String pendingSettleBalance;
}
