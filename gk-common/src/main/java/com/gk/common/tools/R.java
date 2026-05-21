package com.gk.common.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.common.exception.ErrorCode;
import com.gk.common.utils.MsgUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 3453453453423432L;
    /**
     * 编码：0表示成功，其他值表示失败
     */
    private int code = 0;

    /**
     * 消息内容
     */
    private String msg = "success";
    /**
     * 响应数据
     */
    private T data;

    public static <T> R<T> ok() {
        return new R<>();
    }

    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.setData(data);
        return result;
    }

    public static <T> R<T> ok(int code, T data) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setData(data);
        return result;
    }

    public boolean success(){
        return code == 0;
    }

    public static <T> R<T> error() {
        R<T> result = new R<>();
        result.code = ErrorCode.INTERNAL_SERVER_ERROR;
        result.msg = MsgUtils.getMessage(result.code);
        return result;
    }

    public static <T> R<T> error(int code) {
        R<T> result = new R<>();
        result.code = code;
        result.msg = MsgUtils.getMessage(result.code);
        return result;
    }

    public static <T> R<T> error(int code, String... params) {
        R<T> result = new R<>();
        result.code = code;
        result.msg = MsgUtils.getMessage(result.code, params);
        return result;
    }

    public static <T> R<T> error(String msg, Object... args) {
        R<T> result = new R<>();
        result.code = ErrorCode.INTERNAL_SERVER_ERROR;
        result.msg = StringFormat.format(msg, args);
        return result;
    }

    public static <T> R<T> fail() {
        return error(ErrorCode.FAILURE);
    }

    public static <T> R<T> errorMsg(int code, String msg, Object... args) {
        R<T> result = new R<>();
        result.code = code;
        result.msg = StringFormat.format(msg, args);
        return result;
    }
}
