package com.gk.devtools.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.page.PageData;
import com.gk.devtools.dao.FieldTypeDao;
import com.gk.devtools.entity.FieldTypeEntity;
import com.gk.devtools.service.FieldTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 字段类型管理
 *
 * @author Lowen
 */
@Service
public class FieldTypeServiceImpl extends BaseServiceImpl<FieldTypeDao, FieldTypeEntity> implements FieldTypeService {

    @Override
    public PageData<FieldTypeEntity> page(Map<String, Object> params) {
        IPage<FieldTypeEntity> page = baseDao.selectPage(
            getPage(params, Constant.CREATED_AT, false),
            getWrapper(params)
        );
        return new PageData<>(page.getRecords(), page.getTotal());
    }

    @Override
    public Map<String, FieldTypeEntity> getMap() {
        List<FieldTypeEntity> list = baseDao.selectList(null);
        Map<String, FieldTypeEntity> map = new LinkedHashMap<>(list.size());
        for(FieldTypeEntity entity : list){
            map.put(entity.getColumnType().toLowerCase(), entity);
        }
        return map;
    }

    private QueryWrapper<FieldTypeEntity> getWrapper(Map<String, Object> params){
        String attrType = (String)params.get("attrType");
        String columnType = (String)params.get("columnType");

        QueryWrapper<FieldTypeEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(attrType), "attr_type", attrType);
        wrapper.like(StringUtils.isNotEmpty(columnType), "column_type", columnType);

        return wrapper;
    }

    @Override
    public Set<String> getPackageListByTableId(Long tableId) {
        return baseDao.getPackageListByTableId(tableId);
    }

    @Override
    public Set<String> list() {
        return baseDao.list();
    }

}