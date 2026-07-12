package com.gk.infra.datascope;

import java.util.Collections;
import java.util.Map;

public record DataScopeFilter(String sql, Map<String, Object> params) {
    public static DataScopeFilter empty() {
        return new DataScopeFilter("", Collections.emptyMap());
    }

    public boolean isEmpty() {
        return sql == null || sql.isBlank();
    }
}
