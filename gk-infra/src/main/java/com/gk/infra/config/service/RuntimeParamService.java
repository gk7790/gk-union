package com.gk.infra.config.service;

import com.gk.infra.config.dto.RuntimeParamDTO;
import com.gk.infra.config.dto.RuntimeParamUpdateDTO;

import java.util.List;

public interface RuntimeParamService {

    List<RuntimeParamDTO> list();

    void update(String paramCode, RuntimeParamUpdateDTO dto);
}
