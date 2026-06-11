package com.gk.telegram.controller;

import com.gk.telegram.service.TgWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Telegram 入站指令接收(Webhook)
 * <p>
 * Telegram 服务器以 POST 推送 Update 至 {@code /tg/webhook/{botNo}}。
 * 通过请求头 {@code X-Telegram-Bot-Api-Secret-Token} 校验来源, 处理后可直接在响应体返回
 * 一个 Bot API 方法(如 sendMessage)由 Telegram 执行, 无需回调发送接口。
 * </p>
 */
@Tag(name = "Telegram机器人-入站指令")
@RestController
@RequestMapping("/tg/webhook")
@RequiredArgsConstructor
public class TgWebhookController {
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TgWebhookService tgWebhookService;

    @PostMapping("/{botNo}")
    @Operation(summary = "接收Telegram入站指令", description = "Telegram Webhook 回调端点, 校验 secret_token 后幂等处理并分发指令")
    public ResponseEntity<?> receive(
            @Parameter(description = "内部机器人编号(tg_bot.bot_no)") @PathVariable String botNo,
            @RequestHeader(value = SECRET_HEADER, required = false) String secretToken,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {

        String secret = secretToken != null ? secretToken : request.getHeader(SECRET_HEADER);
        TgWebhookService.Result result = tgWebhookService.handle(botNo, secret, rawBody);
        if (!result.authorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<String, Object> reply = result.reply();
        if (reply == null || reply.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(reply);
    }
}
