package com.gk.common.enums;

import lombok.Getter;

import java.util.List;

public interface SysEnum {

    enum sAdmin {
        YES(1),
        NO(0);

        private int value;

        sAdmin(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    @Getter
    enum AppType {
        /**
         * 管理后台
         */
        ADMIN(1, "admin"),
        /**
         * 用户端
         */
        USER(3, "user");

        private final int type;
        private final String label;

        AppType(int type, String label) {
            this.type = type;
            this.label = label;
        }
    }


    @Getter
    enum MenuType {
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

        private final int type;
        private final String label;

        MenuType(int type, String label) {
            this.type = type;
            this.label = label;
        }

        public static List<Integer> enums() {
            return List.of(CATALOG.type, MENU.type);
        }
    }
}
