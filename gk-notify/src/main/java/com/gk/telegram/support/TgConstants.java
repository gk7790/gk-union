package com.gk.telegram.support;

/**
 * Telegram 模块通用常量。
 * <p>
 * 这里集中存放跨多个类共用的数据库枚举值和 Telegram API 固定参数，
 * 避免在服务实现里散落魔法字符串。
 */
public final class TgConstants {
    private TgConstants() {
    }

    /**
     * tg_chat.purpose 可选值。
     */
    public static final class ChatPurpose {
        /** 运维/告警群：接收系统异常、系统预警、风控预警。 */
        public static final String OPS = "OPS";
        /** 业务通知群：接收支付成功、代付成功、代付失败等业务事件。 */
        public static final String NOTIFY = "NOTIFY";
        /** 客服/客户沟通群：预留用途。 */
        public static final String CUSTOMER = "CUSTOMER";

        private ChatPurpose() {
        }
    }

    /**
     * tg_message_task.biz_type 可选值。
     */
    public static final class MessageBizType {
        /** 系统告警类消息。 */
        public static final String SYSTEM_ALERT = "SYSTEM_ALERT";
        /** 业务通知类消息。 */
        public static final String BUSINESS_NOTIFY = "BUSINESS_NOTIFY";

        private MessageBizType() {
        }
    }

    /**
     * Telegram Bot API parse_mode 可选值。
     */
    public static final class ParseMode {
        /** HTML 解析模式。 */
        public static final String HTML = "HTML";

        private ParseMode() {
        }
    }
}
