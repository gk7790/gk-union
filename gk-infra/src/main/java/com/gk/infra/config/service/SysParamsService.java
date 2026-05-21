package com.gk.infra.config.service;


import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.infra.config.dto.SysParamsDTO;
import com.gk.infra.config.entity.SysParamsEntity;

import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
public interface SysParamsService extends BaseService<SysParamsEntity> {

    PageData<SysParamsDTO> page(DataMap params);

    List<SysParamsDTO> list(DataMap params);

    SysParamsDTO get(Long id);

    void save(SysParamsDTO dto);

    void update(SysParamsDTO dto);

    void delete(Long[] ids);

    /**
     * 根据参数编码，获取参数的value值
     *
     * @param paramCode 参数编码
     */
    String getValue(String paramCode);

    /**
     * 根据参数编码，获取value的Object对象
     *
     * @param paramCode 参数编码
     * @param clazz     Object对象
     */
    <T> T getValueObject(String paramCode, Class<T> clazz);

    /**
     * 根据参数编码，更新value
     *
     * @param paramCode  参数编码
     * @param paramValue 参数值
     */
    int updateValueByCode(String paramCode, String paramValue);

    SysParamsEntity selectByParamCode(String paramCode);
}