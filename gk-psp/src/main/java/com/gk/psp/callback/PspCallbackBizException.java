package com.gk.psp.callback;

import com.gk.psp.callback.support.PspCallbackAckMapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class PspCallbackBizException extends RuntimeException {
    private final String ackCode;

    public PspCallbackBizException(String ackCode, String message) {
        super(StringUtils.defaultIfBlank(message, ackCode));
        this.ackCode = StringUtils.defaultIfBlank(ackCode, PspCallbackAckMapper.SYSTEM_ERROR);
    }
}
