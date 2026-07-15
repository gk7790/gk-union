package com.gk.demo.service.impl;

import com.gk.demo.dto.DemoMessageDTO;
import com.gk.demo.service.DemoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoServiceImpl implements DemoService {
    @Override
    public List<DemoMessageDTO> messages() {
        return List.of(
                new DemoMessageDTO("FRAMEWORK", "框架模块加载成功"),
                new DemoMessageDTO("DEMO", "Demo 业务模块加载成功")
        );
    }
}
