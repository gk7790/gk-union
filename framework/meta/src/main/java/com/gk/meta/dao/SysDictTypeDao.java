package com.gk.meta.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.meta.entity.DictType;
import com.gk.meta.entity.SysDictTypeEntity;
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
