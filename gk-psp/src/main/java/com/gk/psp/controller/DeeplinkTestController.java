package com.gk.psp.controller;

import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.psp.support.PspCacheKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.psp.config.PspConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;

@Tag(name = "App唤醒测试")
@RestController
@RequestMapping("tools/deeplink-test")
@RequiredArgsConstructor
public class DeeplinkTestController {
    private static final int EXPIRE_SECONDS = 2 * 60 * 60;
    private static final int TOKEN_BYTES = 24;
    private static final String TOKEN_PREFIX = "dlt_";
    private static final int MAX_GENERATE_ATTEMPTS = 5;

    private final RedisUtils redisUtils;
    private final PspConfigService configService;
    private final SecureRandom random = new SecureRandom();

    @PostMapping("token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成App唤醒测试链接")
    public R<?> createToken(@RequestBody DynMap params, HttpServletRequest request) {
        if (params == null) {
            params = new DynMap();
        }

        String token = newToken();
        String path = "/tools/deeplink-test/" + token;
        String baseUrl = resolveBaseUrl(params.getStr("baseUrl"), request);

        DynMap data = new DynMap();
        data.putAll(params);
        putIfBlank(data, "token", token);
        putIfBlank(data, "path", path);
        putIfBlank(data, "url", baseUrl + path);
        putIfBlank(data, "title", "App 唤醒测试");
        putIfBlank(data, "description",
                "粘贴三方返回的 Deeplink，然后点击拉起 App。页面只在当前浏览器里使用该链接，不会提交或保存。");
        putIfBlank(data, "deeplinkLabel", "Deeplink");
        putIfBlank(data, "deeplinkPlaceholder", "例如：gcash://... 或 maya://...");

        redisUtils.set(PspCacheKeys.deeplinkTest(token), data, EXPIRE_SECONDS);
        return R.ok(data);
    }

    private void putIfBlank(DynMap data, String key, String defaultValue) {
        if (StringUtils.isBlank(data.getStr(key))) {
            data.put(key, defaultValue);
        }
    }

    private String newToken() {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            byte[] bytes = new byte[TOKEN_BYTES];
            random.nextBytes(bytes);
            String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!redisUtils.isKeyExist(PspCacheKeys.deeplinkTest(token))) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate deeplink test token");
    }

    private String resolveBaseUrl(String baseUrl, HttpServletRequest request) {
        if (StringUtils.isNotBlank(baseUrl)) {
            return removeTrailingSlash(baseUrl.trim());
        }
        String apiBaseUrl = configService.domains().getApiBaseUrl();
        if (StringUtils.isNotBlank(apiBaseUrl)) {
            return removeTrailingSlash(apiBaseUrl.trim());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port > 0 && port != 80 && port != 443) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
