package com.gk.common.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BitmaskUtils {
    /**
     * 合并多个 bit
     */
    public static long merge(long... values) {
        long result = 0;
        if (values == null) return 0;

        for (long v : values) {
            result |= v;
        }
        return result;
    }

    public static long mergeListSafe(List<Long> values) {
        if (values == null) return 0L;

        return values.stream()
                .filter(Objects::nonNull)
                .reduce(0L, (a, b) -> a | b);
    }

    /**
     * 合并 enum（要求 enum 有 code() 方法）
     */
    public static <E extends Enum<E> & CodeProvider> long merge(E... enums) {
        long result = 0;
        if (enums == null) return 0;

        for (E e : enums) {
            result |= e.code();
        }
        return result;
    }

    /**
     * 判断是否包含某个 bit
     */
    public static boolean has(long source, long target) {
        return (source & target) != 0;
    }

    /**
     * 判断是否包含 enum
     */
    public static <E extends Enum<E> & CodeProvider> boolean has(long source, E target) {
        return (source & target.code()) != 0;
    }

    /**
     * 是否包含全部
     */
    public static boolean hasAll(long source, long... targets) {
        if (targets == null) return true;

        for (long t : targets) {
            if ((source & t) != t) {
                return false;
            }
        }
        return true;
    }

    /**
     * 添加 bit
     */
    public static long add(long source, long target) {
        return source | target;
    }

    /**
     * 添加 enum
     */
    public static <E extends Enum<E> & CodeProvider> long add(long source, E target) {
        return source | target.code();
    }

    /**
     * 移除 bit
     */
    public static long remove(long source, long target) {
        return source & ~target;
    }

    /**
     * 移除 enum
     */
    public static <E extends Enum<E> & CodeProvider> long remove(long source, E target) {
        return source & ~target.code();
    }

    /**
     * 转换为 enum 数组
     */
    public static <E extends Enum<E> & CodeProvider> E[] toEnums(long source, E[] allEnums) {
        return Arrays.stream(allEnums)
                .filter(e -> has(source, e.code()))
                .toArray(size -> Arrays.copyOf(allEnums, size));
    }

    /**
     * 获取 mask
     */
    public static long of(long... values) {
        return merge(values);
    }

    /**
     * Code 提供接口（你的 enum 可以实现）
     */
    public interface CodeProvider {
        long code();
    }
}
