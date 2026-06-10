package com.gk.infra.datascope;

import com.gk.common.annotation.DataScope;
import com.gk.common.context.ReqContext;
import com.gk.common.enums.DataScopeType;
import com.gk.common.exception.GkException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeSqlBuilderTest {
    private final DataScopeSqlBuilder builder = new DataScopeSqlBuilder();

    @Test
    void autoTenantScopeAddsTenantPredicate() throws Exception {
        DataScope dataScope = annotation("autoTenant");
        ReqContext context = ReqContext.builder()
                .subjectType("TENANT")
                .tenantId(10L)
                .build();

        DataScopeFilter filter = builder.build(dataScope, context);

        assertEquals(" AND t.tenant_id = #{_scopeTenantId}", filter.sql());
        assertEquals(Map.of("_scopeTenantId", 10L), filter.params());
    }

    @Test
    void autoMerchantScopeAddsTenantAndMerchantPredicates() throws Exception {
        DataScope dataScope = annotation("autoMerchant");
        ReqContext context = ReqContext.builder()
                .subjectType("MERCHANT")
                .tenantId(10L)
                .merchantId(20L)
                .build();

        DataScopeFilter filter = builder.build(dataScope, context);

        assertEquals(" AND o.tenant_id = #{_scopeTenantId} AND o.merchant_id = #{_scopeMerchantId}", filter.sql());
        assertEquals(Map.of("_scopeTenantId", 10L, "_scopeMerchantId", 20L), filter.params());
    }

    @Test
    void platformAutoScopeBypassesByDefault() throws Exception {
        DataScope dataScope = annotation("autoPlatform");
        ReqContext context = ReqContext.builder()
                .subjectType("PLATFORM")
                .sAdmin(true)
                .build();

        DataScopeFilter filter = builder.build(dataScope, context);

        assertTrue(filter.isEmpty());
    }

    @Test
    void merchantScopeThrowsWhenMerchantContextMissing() throws Exception {
        DataScope dataScope = annotation("merchantStrict");
        ReqContext context = ReqContext.builder()
                .subjectType("TENANT")
                .tenantId(10L)
                .build();

        assertThrows(GkException.class, () -> builder.build(dataScope, context));
    }

    @Test
    void deptScopeUsesServerSideDeptIdsOnly() throws Exception {
        DataScope dataScope = annotation("dept");
        ReqContext context = ReqContext.builder()
                .deptId(3L)
                .deptIdList(Set.of(4L, 5L))
                .build();

        DataScopeFilter filter = builder.build(dataScope, context);

        assertEquals(" AND u.dept_id IN (3,4,5)", filter.sql());
        assertTrue(filter.params().isEmpty());
    }

    @DataScope(tableAlias = "t")
    void autoTenant() {
    }

    @DataScope(tableAlias = "o")
    void autoMerchant() {
    }

    @DataScope(tableAlias = "p")
    void autoPlatform() {
    }

    @DataScope(value = DataScopeType.MERCHANT, tableAlias = "o")
    void merchantStrict() {
    }

    @DataScope(value = DataScopeType.DEPT, tableAlias = "u")
    void dept() {
    }

    private DataScope annotation(String methodName) throws Exception {
        Method method = getClass().getDeclaredMethod(methodName);
        return method.getAnnotation(DataScope.class);
    }
}
