package com.gk.cloak.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/public")
@Tag(name = "菜单管理")
@AllArgsConstructor
public class LandingController {

    @GetMapping("{code}")
    public Object loading( @PathVariable String code) {
        log.info("code={}", code);
        return "forward:/static/index.html";
    }
}
