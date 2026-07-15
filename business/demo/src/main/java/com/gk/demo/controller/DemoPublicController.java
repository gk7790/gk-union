package com.gk.demo.controller;

import com.gk.common.model.R;
import com.gk.demo.dto.DemoMessageDTO;
import com.gk.demo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo/public")
@RequiredArgsConstructor
public class DemoPublicController {
    private final DemoService demoService;

    @GetMapping("ping")
    public R<String> ping() {
        return R.ok("demo ok");
    }

    @GetMapping("messages")
    public R<List<DemoMessageDTO>> messages() {
        return R.ok(demoService.messages());
    }
}
