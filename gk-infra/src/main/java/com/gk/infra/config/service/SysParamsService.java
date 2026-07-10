package com.gk.infra.config.service;


import com.gk.common.core.service.BaseService;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
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

    PageData<SysParamsDTO> page(DynMap params);

    List<SysParamsDTO> list(DynMap params);

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
     * 根据参数编码，获取value的Object集合
     * @param paramCode 参数编码
     * @param clazz Object对象
     * @return 集合
     */
    <T> List<T> getValueList(String paramCode, Class<T> clazz);
    /**
     * 根据参数编码，更新value
     *
     * @param paramCode  参数编码
     * @param paramValue 参数值
     */
    int updateValueByCode(String paramCode, String paramValue);

    void deleteCacheByCode(String paramCode);

    SysParamsEntity selectByParamCode(String paramCode);
}
