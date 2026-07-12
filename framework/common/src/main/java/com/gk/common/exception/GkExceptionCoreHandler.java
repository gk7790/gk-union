package com.gk.common.exception;

import com.gk.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
public class GkExceptionCoreHandler {

    /**
     * 处理自定义异常
     */
    @ExceptionHandler(GkException.class)
    public R<?> handleRenException(GkException ex) {
        return R.errorMsg(ex.getCode(), ex.getMsg());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingServletRequestParameterException(DuplicateKeyException ex) {
        return R.error(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R<?> handleDuplicateKeyException(DuplicateKeyException ex) {
        return R.error(ErrorCode.DB_RECORD_EXISTS);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleJsonMappingException(HttpMessageNotReadableException ex){
        log.error("请求参数异常: {}", ex.getMessage(), ex);
        return R.error(ErrorCode.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public R<?> handleResourceNotFound(NoResourceFoundException ex) {
        log.error("未找到资源: {}", ex.getResourcePath());
        return R.error(ErrorCode.NOT_FOUND, ex.getResourcePath());
    }



}
