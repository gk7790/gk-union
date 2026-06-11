package com.gk.telegram.support;

/**
 * Telegram HTML(parse_mode=HTML) 文本辅助。
 * <p>
 * Telegram 仅支持有限的 HTML 标签(b/i/u/s/code/pre/a 等)。所有<b>动态/用户可控</b>内容
 * 必须经过 {@link #escape} 转义, 否则其中的 {@code < > &} 会被当作标签解析, 导致 sendMessage 返回 400。
 */
public final class TgHtml {
    private TgHtml() {
    }

    /**
     * 转义 HTML 特殊字符(&、<、>), 用于所有动态/用户可控内容
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 转义任意对象的字符串形式
     */
    public static String escape(Object value) {
        return value == null ? "" : escape(String.valueOf(value));
    }

    /**
     * 加粗(自动转义内容)
     */
    public static String bold(Object value) {
        return "<b>" + escape(value) + "</b>";
    }

    /**
     * 等宽(自动转义内容), 适合单号等可复制内容
     */
    public static String code(Object value) {
        return "<code>" + escape(value) + "</code>";
    }
}
