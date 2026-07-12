package com.gk.auth.config;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * 方法安全表达式处理器: 在默认 root 之上包一层 {@link SuperAdminSecurityExpressionRoot},
 * 使超级管理员跳过所有权限校验。
 * <p>
 * Spring Security 6.3+ 走 {@code createEvaluationContext(Supplier, MethodInvocation)} 路径,
 * 重写 {@code createSecurityExpressionRoot} 已不生效, 故按官方推荐在此替换 rootObject。
 */
public class SuperAdminMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        StandardEvaluationContext context = (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);
        MethodSecurityExpressionOperations delegate = (MethodSecurityExpressionOperations) context.getRootObject().getValue();
        context.setRootObject(new SuperAdminSecurityExpressionRoot(delegate));
        return context;
    }
}
