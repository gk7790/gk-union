package com.gk.common.tools;

import com.gk.common.exception.ErrorCode;
import com.gk.common.utils.MsgUtils;
import lombok.Data;

/**
 * @author Lowen
 */
@Data
public class Result<T> {
    private Boolean success;

    private Integer code;

    private String msg;

    private T data;

    public Result(int code, String message) {
        this.success=false;
        this.code = code;
        this.msg = message;
    }

    public Result(T data) {
        this.success=true;
        this.code = 0;
        this.msg = MsgUtils.getMessage(this.code);
        this.data = data;
    }

    public Result(T data, String message) {
        this.success=true;
        this.code = 0;
        this.msg = message;
        this.data = data;
    }

    public Result(int code, T data) {
        this.success=true;
        this.code = code;
        this.data = data;
    }

    public Result() {}

    public <Y> Y getClazz(Class<Y> clazz) {
        if (clazz.isInstance(this.data)) {
            return clazz.cast(this.data);
        }
        return null;
    }

    public static Result<?> success() {
        Result<?> result = new Result<>();
        result.success=true;
        result.code = 0;
        result.msg = MsgUtils.getMessage(result.code);
        return result;
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(data, message);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> success(int code, T data) {
        return new Result<>(code, data);
    }

    public static <T> Result<T> fail() {
        return fail(ErrorCode.FAILURE);
    }
    public static <T> Result<T> fail(int code) {
        return new Result<>(code, "");
    }

    public static <T> Result<T> fail(int code, String message, Object... args) {
        return new Result<>(code, StringFormat.format(message, args));
    }

    public static <T> Result<T> fail(String message, Object... args) {
        return new Result<>(1, StringFormat.format(message, args));
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 是否失败
     */
    public boolean isFail() {
        return !success;
    }
}
