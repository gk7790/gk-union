package com.gk.demo.service;

import com.gk.demo.dto.DemoMessageDTO;

import java.util.List;

public interface DemoService {
    List<DemoMessageDTO> messages();
}
