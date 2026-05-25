package com.gk.infra.i18n.service;

import com.gk.infra.i18n.dao.SysI18nDao;
import com.gk.infra.i18n.entity.SysI18nEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class I18nService {
    private final SysI18nDao sysI18nDao;

    public Map<String, Object> getMessages(String type, String lang) {
        List<SysI18nEntity> list = sysI18nDao.selectByLang(type, lang);

        Map<String, String> flatMap = list.stream()
                .collect(Collectors.toMap(
                        SysI18nEntity::getI18nKey,
                        SysI18nEntity::getValue,
                        (a, b) -> a
                ));

        return flatToTree(flatMap);
    }

    public static Map<String, Object> flatToTree(Map<String, String> messages) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String[] keys = StringUtils.split(entry.getKey(), ".");
            Map<String, Object> cur = result;

            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];

                if (i == keys.length - 1) {
                    cur.put(k, entry.getValue());
                } else {
                    Object next = cur.get(k);

                    if (!(next instanceof Map)) {
                        next = new HashMap<String, Object>();
                        cur.put(k, next);
                    }

                    cur = (Map<String, Object>) next;
                }
            }
        }

        return result;
    }
}
