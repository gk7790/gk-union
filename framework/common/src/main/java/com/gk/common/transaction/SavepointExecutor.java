package com.gk.common.transaction;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring 事务保存点工具。
 *
 * <p>用于在当前事务中隔离一段可能失败的数据库操作。执行操作前创建保存点；如果操作抛出异常，
 * 则回滚到保存点并继续向上抛出原异常，避免该操作已经执行的部分影响当前事务中的后续逻辑。</p>
 *
 * <p>该工具主要用于“允许出现唯一键冲突”的幂等写入场景。PostgreSQL 在 SQL 执行失败后会将
 * 当前事务标记为失败，即使 Java 代码捕获了 {@code DuplicateKeyException}，后续 SQL 仍然无法执行。
 * 通过回滚到保存点，可以恢复当前事务，使调用方能够按业务规则处理重复数据。</p>
 *
 * <p>注意：本工具不会吞掉业务异常，也不会开启新事务；调用方仍需捕获并判断异常是否可以忽略。</p>
 */
public final class SavepointExecutor {

    /**
     * 工具类不允许实例化。
     */
    private SavepointExecutor() {
    }

    /**
     * 在事务保存点保护下执行数据库操作。
     *
     * <ul>
     *     <li>当前没有 Spring 事务时，直接执行操作。</li>
     *     <li>当前存在 Spring 事务时，先创建保存点再执行操作。</li>
     *     <li>操作失败时回滚到保存点，并重新抛出原异常。</li>
     *     <li>操作结束后释放保存点；释放失败不覆盖原始执行结果。</li>
     * </ul>
     *
     * @param operation 需要执行的数据库操作，不能为空
     */
    public static void run(Runnable operation) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            operation.run();
            return;
        }
        TransactionStatus status = TransactionAspectSupport.currentTransactionStatus();
        Object savepoint = status.createSavepoint();
        try {
            operation.run();
        } catch (RuntimeException | Error exception) {
            try {
                status.rollbackToSavepoint(savepoint);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            try {
                status.releaseSavepoint(savepoint);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
