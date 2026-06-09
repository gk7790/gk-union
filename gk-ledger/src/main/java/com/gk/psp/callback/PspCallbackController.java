package com.gk.psp.callback;

import com.gk.psp.callback.support.PspCallbackBodyCachingFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 两个端点 /pay、/payout，只做转发
 */
@RestController
@RequestMapping("/psp/callback")
@RequiredArgsConstructor
public class PspCallbackController {
    private final PspCallbackService pspCallbackService;

    @PostMapping("/{pspCode}/pay")
    public ResponseEntity<String> payCallback(
            @PathVariable String pspCode,
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        return ResponseEntity.ok(pspCallbackService.handlePayCallback(pspCode, request, rawBody(request, rawBody)));
    }

    @PostMapping("/{pspCode}/payout")
    public ResponseEntity<String> payoutCallback(
            @PathVariable String pspCode,
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        return ResponseEntity.ok(pspCallbackService.handlePayoutCallback(pspCode, request, rawBody(request, rawBody)));
    }

    private String rawBody(HttpServletRequest request, String fallback) {
        Object cached = request.getAttribute(PspCallbackBodyCachingFilter.ATTR_RAW_BODY);
        if (cached instanceof String value && StringUtils.isNotEmpty(value)) {
            return value;
        }
        return fallback;
    }
}
