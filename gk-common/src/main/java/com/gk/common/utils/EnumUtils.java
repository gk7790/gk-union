package com.gk.common.utils;

import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SimpleEnum;
import com.gk.common.enums.StringCodeEnum;

import java.util.Arrays;
import java.util.List;

public class EnumUtils {
    public static <E extends Enum<E> & StringCodeEnum> E fromCode(Class<E> enumClass, String value) {
        return StringCodeEnum.fromCode(enumClass, value);
    }

    public static <E extends Enum<E> & SimpleEnum<?>> List<LabelDTO> toDictList(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new LabelDTO(e.code(), e.label(), e.i18nKey())).toList();
    }
}
