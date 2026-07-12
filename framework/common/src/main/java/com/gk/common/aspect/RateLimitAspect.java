package com.gk.common.aspect;

import com.gk.common.annotation.RateLimit;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    private final RedisUtils redisUtils;


    // 限流的 AOP 切入点，拦截所有带有 @RateLimit 注解的方法
    @Pointcut("@annotation(rateLimit)")
    public void rateLimitPointcut(RateLimit rateLimit) {}

    /**
     * 在方法执行前进行限流判断
     */
    @Before(value = "rateLimitPointcut(rateLimit)", argNames = "joinPoint,rateLimit")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();


        String keyPrefix = rateLimit.key();
        String dynamicPart = parseSpEL(rateLimit.keySpEL(), method, joinPoint.getArgs());
        String redisKey = dynamicPart.isEmpty() ? keyPrefix : keyPrefix + ":" + dynamicPart;

        int limit = rateLimit.count();  // 获取访问限制次数
        int timeout = rateLimit.time();  // 获取时间窗口

        String limitKey = RedisKeys.getLimitKey(redisKey);
        long increment = redisUtils.getIncrement(limitKey, timeout);
        if (increment > limit) {
            throw new GkException(ErrorCode.REQUEST_FREQUENT);
        }
    }


    private String parseSpEL(String spEl, Method method, Object[] args) {
        if (spEl == null || spEl.isEmpty()) return "";

        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames == null) return "";

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(spEl);
        return String.valueOf(expression.getValue(context));
    }


}