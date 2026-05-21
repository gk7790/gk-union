package com.gk.common.tools;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SysFactory {
    public static SseResult sseEmitter(Long timeout) {
        return new SseResult(timeout);
    }

    public static void send(SseEmitter emitter, String name, R<?> r) {
        try {
            emitter.send(SseEmitter.event().name(name).data(r));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public static void sendComplete(SseEmitter emitter, String name, R<?> r) {
        try {
            emitter.send(SseEmitter.event().name(name).data(r));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
