package com.gk.common.tools;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SseResult extends SseEmitter {

    public SseResult(Long timeout) {
        super(timeout);
    }

    public void sendWarn(String msg) {
        sendWarn(msg, null);
    }

    public void sendWarn(String msg, Object data) {
        R<Object> result = new R<>();
        result.setCode(2);
        result.setMsg(msg);
        result.setData(data);
        send("warn", result);
        done();
    }

    public void sendInfo(String msg) {
        sendInfo(msg, null);
    }

    public void sendInfo(Object data) {
        sendInfo(null, data);
    }

    public void sendInfo(String msg, Object data) {
        R<Object> result = new R<>();
        if (ObjectUtils.isEmpty(data)) {
            result.setCode(2);
        }
        result.setMsg(msg);
        result.setData(data);
        send("info", result);
    }

    public void send(String name, int code, String msg, Object data) {
        R<Object> result = new R<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        send(name, result);
    }

    public void send(String name, Object o) {
        try {
            send(SseEmitter.event().name(name).data(o));
        } catch (IOException e) {
            completeWithError(e);
        }
    }

    public void sendDone(Object o) {
        sendDone("", o);
    }

    public void sendDone(String msg, Object o) {
        send("info", 1, msg, o);
        done();
    }

    public void done() {
        send("done", "[DONE]");
        complete();
    }

}
