package com.gk.infra.datascope;

import com.gk.common.annotation.DataScope;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataScopeAspectTest {
    private final DataScopeAspect aspect = new DataScopeAspect(new DataScopeSqlBuilder());

    @AfterEach
    void tearDown() {
        ReqContextHolder.clear();
    }

    @Test
    void applyDataScopeOverridesExternalSqlFilter() throws Throwable {
        ReqContextHolder.set(ReqContext.builder()
                .subjectType("TENANT")
                .tenantId(10L)
                .build());
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.SQL_FILTER, " AND 1 = 1 OR 1 = 1");

        Object result = aspect.applyDataScope(joinPoint(params, "tenantPage"), annotation("tenantPage"));

        assertEquals("ok", result);
        assertEquals(" AND t.tenant_id = #{_scopeTenantId}", params.get(Constant.SQL_FILTER));
        assertEquals(10L, params.get(DataScopeSqlBuilder.PARAM_TENANT_ID));
    }

    @DataScope(tableAlias = "t")
    void tenantPage(Map<String, Object> params) {
    }

    private ProceedingJoinPoint joinPoint(Map<String, Object> params, String methodName) throws Throwable {
        Method method = getClass().getDeclaredMethod(methodName, Map.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{params});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("ok");
        return joinPoint;
    }

    private DataScope annotation(String methodName) throws Exception {
        Method method = getClass().getDeclaredMethod(methodName, Map.class);
        return method.getAnnotation(DataScope.class);
    }
}
