package com.gk.payment.callback;

import com.gk.psp.callback.PspCallbackException;
import com.gk.psp.callback.model.PspCallbackResponse;
import com.gk.psp.callback.support.PspCallbackBodyCachingFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 两个端点 /paypayout，只做转 */
@RestController
@RequestMapping("/psp/callback")
@RequiredArgsConstructor
@Slf4j
public class PspCallbackController {
    private final PspCallbackService pspCallbackService;

    @PostMapping("/{pspCode}/pay")
    public ResponseEntity<String> payCallback(
            @PathVariable String pspCode,
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        return toResponse(pspCallbackService.handlePayCallback(pspCode, request, rawBody(request, rawBody)));
    }

    @PostMapping("/{pspCode}/payout")
    public ResponseEntity<String> payoutCallback(
            @PathVariable String pspCode,
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        return toResponse(pspCallbackService.handlePayoutCallback(pspCode, request, rawBody(request, rawBody)));
    }

    @ExceptionHandler(PspCallbackException.class)
    public ResponseEntity<String> handleCallbackException(PspCallbackException ex) {
        return toResponse(ex.toResponse());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        log.error("Unexpected PSP callback controller error", ex);
        return toResponse(PspCallbackResponse.internalServerError(PspCallbackResponse.DEFAULT_FAIL_BODY));
    }

    private ResponseEntity<String> toResponse(PspCallbackResponse response) {
        return ResponseEntity.status(response.status()).body(response.body());
    }

    private String rawBody(HttpServletRequest request, String fallback) {
        Object cached = request.getAttribute(PspCallbackBodyCachingFilter.ATTR_RAW_BODY);
        if (cached instanceof String value && StringUtils.isNotEmpty(value)) {
            return value;
        }
        return fallback;
    }
}
