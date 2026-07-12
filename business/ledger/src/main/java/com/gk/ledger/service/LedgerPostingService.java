package com.gk.ledger.service;

import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.MerchantBalanceAdjustPostingRequest;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;

public interface LedgerPostingService {

    /**
     * PAYIN_SUCCESS 代收成功入账至待结算账户（非可用）     * 借贷分录     * PSP/CLEARING settleAmount + feeAmount；历沙箱订单没有pspAccountId时回退 INTERNAL/CLEARING
     * MERCHANT/PENDING_SETTLE settleAmount
     * INTERNAL/FEE_INCOME merchantFeeAmount
     */
    LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request);

    /**
     * SETTLE_RELEASE 待结算释放至商户可用     * MERCHANT/PENDING_SETTLE settleAmount
     * MERCHANT/AVAILABLE settleAmount
     */
    LedgerPostingResult releasePaySettle(PaySuccessPostingRequest request);

    /**
     * 代付冻结
     * 代付 = 商户把钱付出去。下单时先冻结，要付的总额"（totalDebitAmount = amount + 手续费）从可用挪到冻结，防止商户超额/重复代付     * 入参 PayoutPostingRequest：amount、merchantFeeAmount、totalDebitAmount     * 分录     *      商户可用 AVAILABLE totalDebitAmount
     *      商户冻结 FROZEN totalDebitAmount
     *      同时写一ledger_hold（status=HOLDING，hold_amount=remaining_amount=totalDebitAmount）     * 返回 journalNo + holdNo（唯一一个会holdNo 的方法），回填到订单holdNo / freezeJournalNo     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult freezePayout(PayoutPostingRequest request);

    /**
     * 代付成功（扣减冻结）
     * PSP 回调代付成功，钱真的打出去了。把之前冻的消掉，确认手续费收入     * 分录     *      商户冻结 FROZEN totalDebitAmount
     *      PSP 清算 CLEARING amount
     *      内部手续费收merchantFeeAmount
     *      对应 ledger_hold CONSUMED，回consume_journal_no 和订单的 successJournalNo。返journalNo     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult postPayoutSuccess(PayoutPostingRequest request);

    /**
     * 代付失败（解冻）
     * PSP 回调代付失败，钱没出去。把冻结的钱原路退回可用余额     * 分录     *      商户冻结 FROZEN totalDebitAmount
     *      商户可用 AVAILABLE totalDebitAmount
     *      对应 ledger_hold RELEASED，回last_release_journal_no 和订单的 releaseJournalNo。返journalNo     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult releasePayout(PayoutPostingRequest request);

    /**
     * 运营手工充值入账     *
     * 将一笔商户余额调整单过账到总账     * 1. 校验租户、商户、币种、金额、调整单号等必填参数     * 2. 使用调整单号 + 事件类型做幂等控制，避免重复充值；
     * 3. 生成 ledger_journal 账务凭证，source_type = MANUAL     * 4. 生成两条 ledger_entry 分录     *    - 借：INTERNAL/CLEARING 内部清算账户
     *    - 贷：MERCHANT/AVAILABLE 商户可用余额账户
     * 5. 更新 ledger_balance，使商户可用余额增加     * 6. 返回入账凭证journalNo，供 merchant_balance_adjust_order 回填     * 注意：该方法只处理“充增加余额”场景     * 如果后续要统一支持充值、扣减、冲正、补账，建议使用更通用     * postMerchantBalanceAdjust(...)，通过 adjustType 区分 RECHARGE/DEDUCT/REVERSE/SUPPLEMENT     */
    LedgerPostingResult postMerchantBalanceAdjust(MerchantBalanceAdjustPostingRequest request);
}
