package com.gk.ledger.service;

import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;

public interface LedgerPostingService {

    /**
     * PAY_SUCCESS — 代收成功入账至待结算账户（非可用）。
     * 借贷分录：
     * SYSTEM_CLEARING 借 settleAmount + feeAmount
     * MERCHANT_PENDING_SETTLE 贷 settleAmount
     * PLATFORM_FEE_INCOME 贷 merchantFeeAmount
     */
    LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request);

    /**
     * SETTLE_RELEASE — 待结算释放至商户可用。
     * MERCHANT_PENDING_SETTLE 借 settleAmount
     * MERCHANT_AVAILABLE 贷 settleAmount
     */
    LedgerPostingResult releasePaySettle(PaySuccessPostingRequest request);

    /**
     * 代付冻结
     * 代付 = 商户把钱付出去。下单时先冻结，把"要付的总额"（totalDebitAmount = amount + 手续费）从可用挪到冻结，防止商户超额/重复代付。
     * 入参 PayoutPostingRequest：amount、merchantFeeAmount、totalDebitAmount。
     * 分录：
     *      商户可用 MERCHANT_AVAILABLE ↓ totalDebitAmount
     *      商户冻结 MERCHANT_FROZEN ↑ totalDebitAmount
     *      同时写一条 ledger_hold（status=HOLDING，hold_amount=remaining_amount=totalDebitAmount）。
     * 返回 journalNo + holdNo（唯一一个会带 holdNo 的方法），回填到订单的 holdNo / freezeJournalNo。
     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult freezePayout(PayoutPostingRequest request);

    /**
     * 代付成功（扣减冻结）
     * PSP 回调代付成功，钱真的打出去了。把之前冻的钱"消耗"掉，确认手续费收入。
     * 分录：
     *      商户冻结 MERCHANT_FROZEN ↓ totalDebitAmount
     *      PSP 清算 PSP_CLEARING ↑ amount
     *      平台手续费收入 ↑ merchantFeeAmount
     *      对应 ledger_hold → CONSUMED，回填 consume_journal_no 和订单的 successJournalNo。返回 journalNo。
     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult postPayoutSuccess(PayoutPostingRequest request);

    /**
     * 代付失败（解冻）
     * PSP 回调代付失败，钱没出去。把冻结的钱原路退回可用余额。
     * 分录：
     *      商户冻结 MERCHANT_FROZEN ↓ totalDebitAmount
     *      商户可用 MERCHANT_AVAILABLE ↑ totalDebitAmount
     *      对应 ledger_hold → RELEASED，回填 last_release_journal_no 和订单的 releaseJournalNo。返回 journalNo。
     * @param request 请求
     * @return 结果
     */
    LedgerPostingResult releasePayout(PayoutPostingRequest request);
}
