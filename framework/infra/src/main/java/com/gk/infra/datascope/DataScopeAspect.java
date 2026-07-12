package com.gk.infra.datascope;

import com.gk.common.annotation.DataScope;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Aspect
@Component
@Order(50)
@RequiredArgsConstructor
public class DataScopeAspect {
    private final DataScopeSqlBuilder sqlBuilder;

    @Around("@annotation(dataScope)")
    public Object applyDataScope(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        ReqContext context = ReqContextHolder.get();
        DataScopeFilter filter = sqlBuilder.build(dataScope, context);
        Map<String, Object> params = findParams(joinPoint.getArgs());
        if (params == null) {
            if (!filter.isEmpty() && dataScope.strict()) {
                throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
            }
            return joinPoint.proceed();
        }

        // sqlFilter is always system-generated. Never trust a value provided by the request.
        params.remove(Constant.SQL_FILTER);
        params.remove(DataScopeSqlBuilder.PARAM_TENANT_ID);
        params.remove(DataScopeSqlBuilder.PARAM_MERCHANT_ID);
        params.remove(DataScopeSqlBuilder.PARAM_USER_ID);

        if (!filter.isEmpty()) {
            params.putAll(filter.params());
            params.put(Constant.SQL_FILTER, filter.sql());
            log.debug("Applied data scope on {}: {}", methodName(joinPoint), filter.sql());
        }

        return joinPoint.proceed();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findParams(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        }
        return null;
    }

    private String methodName(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature signature) {
            Method method = signature.getMethod();
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        return joinPoint.getSignature().toShortString();
    }
}
