package com.gk.ledger.service;

import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;

public interface LedgerPostingService {

    /**
     * PAY_SUCCESS
     * 代收成功入账
     * 代收 = 买家付钱进来给商户。PSP 收到钱后，平台给商户记一笔可用余额（扣掉手续费）。
     * 入参 PaySuccessPostingRequest：amount（订单额）、merchantFeeAmount（手续费）、settleAmount（待结算 = amount − 手续费）。
     * 借贷分录（示意）：
     * PSP 清算账户 PSP_CLEARING ← 收到 amount
     * 商户可用 MERCHANT_AVAILABLE ← 增加 settleAmount
     * 平台手续费收入 ← 增加 merchantFeeAmount
     * 返回只有 journalNo（无 holdNo，代收不涉及冻结）。
     * @param request 参数
     */
    LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request);

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
