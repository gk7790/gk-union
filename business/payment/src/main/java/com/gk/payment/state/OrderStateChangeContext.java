package com.gk.payment.state;

public record OrderStateChangeContext(String eventType,
                                      String reason,
                                      String operatorType,
                                      String operatorId,
                                      String requestId,
                                      String traceId) {
    public static OrderStateChangeContext system(String eventType, String reason) {
        return new OrderStateChangeContext(eventType, reason, "SYSTEM", null, null, null);
    }
}
 