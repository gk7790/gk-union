package com.gk.payment.notify;

import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.common.enums.BizTypeEnum;
import com.gk.payment.enums.MerchantNotifyStatusEnum;
import com.gk.payment.enums.MerchantNotifyTaskStatusEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 维护订单表上的下游商户通知状态(与 merchant_notify_task 冗余同步)。
 */
@Service
@RequiredArgsConstructor
public class MerchantOrderNotifyStatusService {

    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;

    public String initialStatus(String notifyUrl) {
        return StringUtils.isBlank(notifyUrl) ? MerchantNotifyStatusEnum.NONE.code() : null;
    }

    public void onTaskCreated(String bizType, Long orderId, Long taskId) {
        if (orderId == null || taskId == null) {
            return;
        }
        updateOrder(bizType, orderId, MerchantNotifyStatusEnum.PENDING.code(), null, taskId);
    }

    public void syncFromTask(MerchantNotifyTaskEntity task) {
        if (task == null || task.getBizId() == null) {
            return;
        }
        String orderStatus = toOrderStatus(task.getStatus());
        if (orderStatus == null) {
            return;
        }
        Instant notifyAt = resolveNotifyAt(task, orderStatus);
        updateOrder(task.getBizType(), task.getBizId(), orderStatus, notifyAt, task.getId());
    }

    private String toOrderStatus(String taskStatus) {
        if (MerchantNotifyTaskStatusEnum.SUCCESS.matches(taskStatus)) {
            return MerchantNotifyStatusEnum.SUCCESS.code();
        }
        if (MerchantNotifyTaskStatusEnum.DEAD.matches(taskStatus)) {
            return MerchantNotifyStatusEnum.FAILED.code();
        }
        if (StringUtils.isNotBlank(taskStatus)) {
            return MerchantNotifyStatusEnum.PENDING.code();
        }
        return null;
    }

    private Instant resolveNotifyAt(MerchantNotifyTaskEntity task, String orderStatus) {
        if (MerchantNotifyStatusEnum.SUCCESS.code().equals(orderStatus)) {
            return task.getSuccessAt() != null ? task.getSuccessAt() : task.getLastAttemptAt();
        }
        if (MerchantNotifyStatusEnum.FAILED.code().equals(orderStatus)) {
            return task.getDeadAt() != null ? task.getDeadAt() : task.getLastAttemptAt();
        }
        return task.getLastAttemptAt();
    }

    private void updateOrder(String bizType, Long orderId, String status, Instant notifyAt, Long taskId) {
        if (BizTypeEnum.PAY_ORDER.matches(bizType)) {
            PayOrderEntity update = new PayOrderEntity();
            update.setId(orderId);
            update.setMerchantNotifyStatus(status);
            update.setMerchantNotifyAt(notifyAt);
            update.setMerchantNotifyTaskId(taskId);
            payOrderDao.updateById(update);
            return;
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType)) {
            PayoutOrderEntity update = new PayoutOrderEntity();
            update.setId(orderId);
            update.setMerchantNotifyStatus(status);
            update.setMerchantNotifyAt(notifyAt);
            update.setMerchantNotifyTaskId(taskId);
            payoutOrderDao.updateById(update);
        }
    }
}
