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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GkExceptionHandler extends GkExceptionCoreHandler {
    private final LogErrorService logErrorService;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public R<?> handleException(AuthorizationDeniedException ex) {
        return R.error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        saveLog(ex);
        return R.error();
    }

    /**
     * 保存异常日志
     */
    private void saveLog(Exception ex) {
        if (StringUtils.isNotBlank(ex.getMessage()) && ex.getMessage().contains("Broken pipe")) {
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
}
