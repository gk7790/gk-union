package com.gk.telegram.support;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Telegram HTML(parse_mode=HTML) 文本辅助。
 * <p>
 * Telegram 仅支持有限的 HTML 标签(b/i/u/s/code/pre/a 等)。所有<b>动态/用户可控</b>内容
 * 必须经过 {@link #escape} 转义, 否则其中的 {@code < > &} 会被当作标签解析, 导致 sendMessage 返回 400。
 */
public final class TgHtml {
    private static final Pattern TAG_PATTERN = Pattern.compile("<([^<>]+)>");
    private static final Pattern HREF_ATTR_PATTERN = Pattern.compile("href\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_ATTR_PATTERN = Pattern.compile("class\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final Set<String> SIMPLE_TAGS = Set.of(
            "b", "strong", "i", "em", "u", "ins", "s", "strike", "del",
            "tg-spoiler", "pre", "blockquote"
    );

    /**
     * 工具类不允许实例化。
     */
    private TgHtml() {
    }

    /**
     * 转义 HTML 特殊字符(&、<、>), 用于所有动态/用户可控内容
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        // Telegram HTML 模式只需要转义这三个字符，避免用户输入被当成标签解析。
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

    /**
     * 校验文本是否符合 Telegram Bot API 的 HTML parse_mode 基本规则。
     * <p>
     * 这里不做自动清洗，发现不支持的标签或属性直接拒绝，避免任务入库后 Telegram 返回 400。
     */
    public static void assertTelegramHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("Telegram HTML content is required");
        }
        Matcher matcher = TAG_PATTERN.matcher(html);
        StringBuilder textWithoutTags = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            textWithoutTags.append(html, lastEnd, matcher.start());
            assertTelegramTag(matcher.group(1).trim());
            lastEnd = matcher.end();
        }
        textWithoutTags.append(html.substring(lastEnd));
        if (textWithoutTags.indexOf("<") >= 0 || textWithoutTags.indexOf(">") >= 0) {
            throw unsupportedTag("unescaped < or >");
        }
    }

    private static void assertTelegramTag(String rawTag) {
        if (rawTag.isBlank() || rawTag.startsWith("!") || rawTag.startsWith("?")) {
            throw unsupportedTag(rawTag);
        }
        boolean closing = rawTag.startsWith("/");
        String body = closing ? rawTag.substring(1).trim() : rawTag;
        if (body.endsWith("/")) {
            body = body.substring(0, body.length() - 1).trim();
        }
        String tagName = tagName(body);
        String attrs = body.substring(tagName.length()).trim();
        String normalized = tagName.toLowerCase(Locale.ROOT);
        if (closing) {
            if (!attrs.isBlank() || !isSupportedTag(normalized)) {
                throw unsupportedTag(rawTag);
            }
            return;
        }
        if (SIMPLE_TAGS.contains(normalized)) {
            assertSimpleAttrs(normalized, attrs, rawTag);
            return;
        }
        if ("code".equals(normalized)) {
            assertCodeAttrs(attrs, rawTag);
            return;
        }
        if ("a".equals(normalized)) {
            assertLinkAttrs(attrs, rawTag);
            return;
        }
        if ("span".equals(normalized)) {
            assertSpoilerSpan(attrs, rawTag);
            return;
        }
        throw unsupportedTag(rawTag);
    }

    private static String tagName(String body) {
        int index = 0;
        while (index < body.length()) {
            char ch = body.charAt(index);
            if (Character.isWhitespace(ch) || ch == '/') {
                break;
            }
            index++;
        }
        if (index == 0) {
            throw unsupportedTag(body);
        }
        return body.substring(0, index);
    }

    private static boolean isSupportedTag(String tagName) {
        return SIMPLE_TAGS.contains(tagName)
                || "a".equals(tagName)
                || "code".equals(tagName)
                || "span".equals(tagName);
    }

    private static void assertSimpleAttrs(String tagName, String attrs, String rawTag) {
        if ("blockquote".equals(tagName) && "expandable".equalsIgnoreCase(attrs)) {
            return;
        }
        if (!attrs.isBlank()) {
            throw unsupportedTag(rawTag);
        }
    }

    private static void assertCodeAttrs(String attrs, String rawTag) {
        if (attrs.isBlank()) {
            return;
        }
        Matcher matcher = CLASS_ATTR_PATTERN.matcher(attrs);
        if (!matcher.matches() || !matcher.group(2).startsWith("language-")) {
            throw unsupportedTag(rawTag);
        }
    }

    private static void assertLinkAttrs(String attrs, String rawTag) {
        Matcher matcher = HREF_ATTR_PATTERN.matcher(attrs);
        if (!matcher.matches()) {
            throw unsupportedTag(rawTag);
        }
        String href = matcher.group(2).trim().toLowerCase(Locale.ROOT);
        if (href.startsWith("javascript:")) {
            throw unsupportedTag(rawTag);
        }
    }

    private static void assertSpoilerSpan(String attrs, String rawTag) {
        Matcher matcher = CLASS_ATTR_PATTERN.matcher(attrs);
        if (!matcher.matches() || !"tg-spoiler".equals(matcher.group(2))) {
            throw unsupportedTag(rawTag);
        }
    }

    private static IllegalArgumentException unsupportedTag(String rawTag) {
        return new IllegalArgumentException("Unsupported Telegram HTML tag or attribute: <" + rawTag + ">");
    }
}
