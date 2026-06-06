package com.gk.common.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum MenuTypeEnum implements CodeEnum<Integer> {
    /**
     * 目录
     */
    CATALOG(1, "catalog"),
    /**
     * 菜单
     */
    MENU(2, "menu"),
    /**
     * 内嵌
     */
    EMBEDDED(3, "embedded"),
    /**
     * 链接
     */
    LINK(4, "link"),
    /**
     * 按钮
     */
    BUTTON(5, "button");

    private final int code;
    private final String label;

    MenuTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static List<Integer> enums() {
        return List.of(CATALOG.code, MENU.code);
    }

    public static List<Integer> auth() {
        return List.of(BUTTON.code);
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }
}
