package com.gk.telegram.support;

/**
 * Telegram MarkdownV2(parse_mode=MarkdownV2) 文本辅助。
 * <p>
 * 所有动态/用户可控内容必须经过 {@link #escape(Object)} 转义，否则 Telegram
 * 会因为 MarkdownV2 特殊字符未转义而返回 400。
 */
public final class TgMarkdownV2 {
    private TgMarkdownV2() {
    }

    /**
     * 转义 MarkdownV2 特殊字符。
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ("\\_*[]()~`>#+-=|{}.!".indexOf(ch) >= 0) {
                builder.append('\\');
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    /**
     * 转义任意对象的字符串形式。
     */
    public static String escape(Object value) {
        return value == null ? "" : escape(String.valueOf(value));
    }

    /**
     * 加粗(自动转义内容)。
     */
    public static String bold(Object value) {
        return "*" + escape(value) + "*";
    }

    /**
     * 等宽(自动转义内容)，适合单号等可复制内容。
     */
    public static String code(Object value) {
        return "`" + escape(value) + "`";
    }
}
