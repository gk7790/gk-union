package com.gk.infra.dict.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.dict.entity.DictType;
import com.gk.infra.dict.entity.SysDictTypeEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 字典类型
 *
 * @author Lowen
 */
@Mapper
public interface SysDictTypeDao extends BaseDao<SysDictTypeEntity> {

    /**
     * 字典类型列表
     */
    List<DictType> getDictTypeList();

}
