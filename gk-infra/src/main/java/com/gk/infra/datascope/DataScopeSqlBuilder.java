package com.gk.infra.datascope;

import com.gk.common.annotation.DataScope;
import com.gk.common.context.ReqContext;
import com.gk.common.enums.DataScopeType;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DataScopeSqlBuilder {
    public static final String PARAM_TENANT_ID = "_scopeTenantId";
    public static final String PARAM_MERCHANT_ID = "_scopeMerchantId";
    public static final String PARAM_USER_ID = "_scopeUserId";

    private static final String SUBJECT_PLATFORM = "PLATFORM";
    private static final String SUBJECT_TENANT = "TENANT";
    private static final String SUBJECT_MERCHANT = "MERCHANT";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public DataScopeFilter build(DataScope dataScope, ReqContext context) {
        if (dataScope == null || dataScope.value() == DataScopeType.NONE) {
            return DataScopeFilter.empty();
        }
        ReqContext safeContext = context == null ? new ReqContext() : context;
        DataScopeType type = resolveType(dataScope, safeContext);
        if (type == DataScopeType.NONE) {
            return DataScopeFilter.empty();
        }

        return switch (type) {
            case TENANT -> tenantFilter(dataScope, safeContext);
            case MERCHANT -> merchantFilter(dataScope, safeContext);
            case DEPT -> deptFilter(dataScope, safeContext);
            case SELF -> selfFilter(dataScope, safeContext);
            case AUTO, NONE -> DataScopeFilter.empty();
        };
    }

    private DataScopeType resolveType(DataScope dataScope, ReqContext context) {
        DataScopeType configured = dataScope.value();
        if (configured != DataScopeType.AUTO) {
            return configured;
        }

        String subjectType = StringUtils.upperCase(context.getSubjectType());
        if (SUBJECT_PLATFORM.equals(subjectType)) {
            return dataScope.platformBypass() ? DataScopeType.NONE : DataScopeType.TENANT;
        }
        if (SUBJECT_TENANT.equals(subjectType)) {
            return DataScopeType.TENANT;
        }
        if (SUBJECT_MERCHANT.equals(subjectType)) {
            return DataScopeType.MERCHANT;
        }
        if (dataScope.strict()) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
        return DataScopeType.NONE;
    }

    private DataScopeFilter tenantFilter(DataScope dataScope, ReqContext context) {
        Long tenantId = context.getTenantId();
        if (tenantId == null) {
            return missingRequiredContext(dataScope);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_TENANT_ID, tenantId);
        return new DataScopeFilter(" AND " + column(dataScope.tableAlias(), dataScope.tenantColumn()) + " = #{" + PARAM_TENANT_ID + "}", params);
    }

    private DataScopeFilter merchantFilter(DataScope dataScope, ReqContext context) {
        Long tenantId = context.getTenantId();
        Long merchantId = context.getMerchantId();
        if (tenantId == null || merchantId == null) {
            return missingRequiredContext(dataScope);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_TENANT_ID, tenantId);
        params.put(PARAM_MERCHANT_ID, merchantId);
        String sql = " AND " + column(dataScope.tableAlias(), dataScope.tenantColumn()) + " = #{" + PARAM_TENANT_ID + "}"
                + " AND " + column(dataScope.tableAlias(), dataScope.merchantColumn()) + " = #{" + PARAM_MERCHANT_ID + "}";
        return new DataScopeFilter(sql, params);
    }

    private DataScopeFilter deptFilter(DataScope dataScope, ReqContext context) {
        Set<Long> deptIds = new TreeSet<>();
        if (context.getDeptId() != null) {
            deptIds.add(context.getDeptId());
        }
        if (CollectionUtils.isNotEmpty(context.getDeptIdList())) {
            deptIds.addAll(context.getDeptIdList());
        }
        if (deptIds.isEmpty()) {
            return missingRequiredContext(dataScope);
        }

        String ids = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return new DataScopeFilter(" AND " + column(dataScope.tableAlias(), dataScope.deptColumn()) + " IN (" + ids + ")", Map.of());
    }

    private DataScopeFilter selfFilter(DataScope dataScope, ReqContext context) {
        Long userId = context.getUserId();
        if (userId == null) {
            return missingRequiredContext(dataScope);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_USER_ID, userId);
        return new DataScopeFilter(" AND " + column(dataScope.tableAlias(), dataScope.userColumn()) + " = #{" + PARAM_USER_ID + "}", params);
    }

    private DataScopeFilter missingRequiredContext(DataScope dataScope) {
        if (dataScope.strict()) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
        return DataScopeFilter.empty();
    }

    private String column(String tableAlias, String column) {
        String safeColumn = validateIdentifier(column);
        if (StringUtils.isBlank(tableAlias)) {
            return safeColumn;
        }
        return validateIdentifier(tableAlias) + "." + safeColumn;
    }

    private String validateIdentifier(String value) {
        if (StringUtils.isBlank(value) || !IDENTIFIER.matcher(value).matches()) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
        return value;
    }
}
