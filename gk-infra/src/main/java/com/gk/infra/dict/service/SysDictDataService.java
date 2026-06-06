package com.gk.infra.dict.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.infra.dict.dto.SysDictDataDTO;
import com.gk.infra.dict.entity.SysDictDataEntity;

/**
 * 数据字典
 *
 * @author Lowen
 */
public interface SysDictDataService extends BaseService<SysDictDataEntity> {

    PageData<SysDictDataDTO> page(DynMap params);

    PageData<SysDictDataDTO> getPage(DynMap params);

    SysDictDataDTO get(Long id);

    void save(SysDictDataDTO dto);

    void update(SysDictDataDTO dto);

    void delete(Long[] ids);

}