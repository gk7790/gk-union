package com.gk.meta.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.meta.dto.SysDictTypeDTO;
import com.gk.meta.entity.DictType;
import com.gk.meta.entity.SysDictTypeEntity;

import java.util.List;

/**
 * 数据字典
 *
 * @author Lowen
 */
public interface SysDictTypeService extends BaseService<SysDictTypeEntity> {

    PageData<SysDictTypeDTO> page(DynMap params);

    SysDictTypeDTO get(Long id);

    void save(SysDictTypeDTO dto);

    void update(SysDictTypeDTO dto);

    void delete(Long[] ids);

    /**
     * 获取所有字典
     */
    List<DictType> getAllList();

    /**
     * 字典类型列表
     */
    List<DictType> getDictTypeList();

    List<LabelDTO> getDictList(String dictType);
}