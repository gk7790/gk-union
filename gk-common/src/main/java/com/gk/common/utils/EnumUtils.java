package com.gk.common.utils;

import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SimpleEnum;

import java.util.Arrays;
import java.util.List;

public class EnumUtils {
    public static <E extends Enum<E> & SimpleEnum<?>> List<LabelDTO> toDictList(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new LabelDTO(String.valueOf(e.code()), e.label(), e.i18nKey())).toList();
    }
}
