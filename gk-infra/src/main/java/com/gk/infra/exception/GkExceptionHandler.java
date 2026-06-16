package com.gk.infra.exception;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSONObject;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.ExceptionUtils;
import com.gk.common.exception.GkExceptionCoreHandler;
import com.gk.common.model.R;
import com.gk.infra.log.entity.LogErrorEntity;
import com.gk.infra.log.service.LogErrorService;
import com.gk.infra.telegram.TgAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GkExceptionHandler extends GkExceptionCoreHandler {
    private final LogErrorService logErrorService;
    private final ObjectProvider<TgAlertService> tgAlertServiceProvider;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public R<?> handleException(AuthorizationDeniedException ex) {
        return R.error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception ex) {
        if (isBrokenPipe(ex)) {
            log.debug(ex.getMessage(), ex);
        } else {
            log.error(ex.getMessage(), ex);
        }
        saveLog(ex);
        sendSystemErrorAlert(ex);
        return R.error();
    }

    /**
     * 保存异常日志
     */
    private void saveLog(Exception ex) {
        if (isBrokenPipe(ex)) {
            return;
        }

        ReqContext context = ReqContextHolder.get();

        LogErrorEntity log = new LogErrorEntity();

        log.setUserId(context.getUserId());
        log.setUsername(context.getUsername());
        log.setTenantId(context.getTenantId());
        log.setRequestUri(context.getUri());
        log.setTraceId(context.getTraceId());
        log.setIp(context.getIp());
        log.setUserAgent(context.getUserAgent());
        log.setBrowser(context.getBrowser());
        log.setOs(context.getOs());
        log.setCountry(context.getCountry());
        log.setCity(context.getCity());
        log.setDeviceType(context.getDevice());
        log.setErrorMessage(ex.getMessage());
        log.setStackTrace(ExceptionUtils.getErrorStackTrace(ex));
        if (MapUtil.isNotEmpty(context.getHeaders())) {
            log.setRequestHeaders(JSONObject.toJSONString(context.getHeaders()));
        }

        logErrorService.asyncAdd(log);
    }

    /**
     * 将未被业务异常处理器消费的系统异常推送到 Telegram 平台告警群。
     */
    private void sendSystemErrorAlert(Exception ex) {
        if (isBrokenPipe(ex)) {
            return;
        }
        TgAlertService tgAlertService = tgAlertServiceProvider.getIfAvailable();
        if (tgAlertService == null) {
            return;
        }

        ReqContext context = ReqContextHolder.get();
        try {
            tgAlertService.systemError(context.getTenantId(), context.getMerchantId(),
                    "系统异常", buildAlertContent(context, ex), context.getTraceId());
        } catch (Exception alertEx) {
            log.warn("send Telegram system error alert failed: {}", alertEx.getMessage());
        }
    }

    /**
     * 告警正文保留请求入口和异常摘要，租户/商户展示由 TgAlertService 根据接收群层级处理。
     */
    private String buildAlertContent(ReqContext context, Exception ex) {
        StringBuilder content = new StringBuilder();
        appendRequestLine(content, context);
        content.append("异常: ").append(ex.getClass().getSimpleName());
        if (StringUtils.isNotBlank(ex.getMessage())) {
            content.append('\n').append("消息: ").append(ex.getMessage());
        }
        if (context.getUserId() != null) {
            content.append('\n').append("用户ID: ").append(context.getUserId());
        }
        return content.toString();
    }

    /**
     * 组装请求方法和地址，便于在 Telegram 告警里快速定位接口入口。
     */
    private void appendRequestLine(StringBuilder content, ReqContext context) {
        if (StringUtils.isBlank(context.getMethod()) && StringUtils.isBlank(context.getUri())) {
            return;
        }
        content.append(StringUtils.defaultIfBlank(context.getMethod(), "-"))
                .append(' ')
                .append(StringUtils.defaultIfBlank(context.getUri(), "-"))
                .append('\n');
    }

    /**
     * 客户端断连属于噪声异常，不记录错误日志，也不触发系统告警。
     */
    private boolean isBrokenPipe(Exception ex) {
        return StringUtils.isNotBlank(ex.getMessage()) && ex.getMessage().contains("Broken pipe");
    }
}
