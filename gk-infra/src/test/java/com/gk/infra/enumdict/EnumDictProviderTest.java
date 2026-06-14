package com.gk.infra.enumdict;

import com.gk.common.annotation.EnumDict;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SimpleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumDictProviderTest {

    @Test
    void listReturnsOnlyVisibleAnnotatedSimpleEnums() {
        EnumDictProvider provider = new EnumDictProvider("com.gk.infra.enumdict");

        Map<String, List<LabelDTO>> dicts = provider.list(null);

        assertTrue(dicts.containsKey("sampleVisible"));
        assertFalse(dicts.containsKey("sampleHidden"));
        assertFalse(dicts.containsKey("samplePlain"));
        assertEquals("A", dicts.get("sampleVisible").getFirst().getValue());
    }

    @EnumDict("sampleVisible")
    enum SampleVisibleEnum implements SimpleEnum<String> {
        A("A", "A label", "enum.sample.a");

        private final String code;
        private final String label;
        private final String i18nKey;

        SampleVisibleEnum(String code, String label, String i18nKey) {
            this.code = code;
            this.label = label;
            this.i18nKey = i18nKey;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public String i18nKey() {
            return i18nKey;
        }
    }

    @EnumDict(value = "sampleHidden", visible = false)
    enum SampleHiddenEnum implements SimpleEnum<String> {
        B("B", "B label", "enum.sample.b");

        private final String code;
        private final String label;
        private final String i18nKey;

        SampleHiddenEnum(String code, String label, String i18nKey) {
            this.code = code;
            this.label = label;
            this.i18nKey = i18nKey;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public String i18nKey() {
            return i18nKey;
        }
    }

    @EnumDict("samplePlain")
    enum SamplePlainEnum {
        C
    }
}
