package com.gk.common.exception;


import com.gk.common.utils.MsgUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义异常
 *
 * @author Lowen
 */
@Setter
@Getter
public class GkException extends RuntimeException {

    private int code;
	private String msg;

	public GkException(int code) {
		this.code = code;
		this.msg = MsgUtils.getMessage(code);
	}

	public GkException(int code, String... params) {
		this.code = code;
		this.msg = MsgUtils.getMessage(code, params);
	}

	public GkException(int code, Throwable e) {
		super(e);
		this.code = code;
		this.msg = MsgUtils.getMessage(code);
	}

	public GkException(int code, Throwable e, String... params) {
		super(e);
		this.code = code;
		this.msg = MsgUtils.getMessage(code, params);
	}

	public GkException(String msg) {
		super(msg);
		this.code = ErrorCode.INTERNAL_SERVER_ERROR;
		this.msg = msg;
	}

	public GkException(String msg, Throwable e) {
		super(msg, e);
		this.code = ErrorCode.INTERNAL_SERVER_ERROR;
		this.msg = msg;
	}

}