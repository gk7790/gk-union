package com.gk.infra.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.dict.dao.SysDictDataDao;
import com.gk.infra.dict.dao.SysDictTypeDao;
import com.gk.infra.dict.dto.SysDictTypeDTO;
import com.gk.infra.dict.entity.DictData;
import com.gk.infra.dict.entity.DictType;
import com.gk.infra.dict.entity.SysDictTypeEntity;
import com.gk.infra.dict.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 字典类型
 *
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends BaseServiceImpl<SysDictTypeDao, SysDictTypeEntity> implements SysDictTypeService {
    private final SysDictDataDao sysDictDataDao;


    @Override
    public PageData<SysDictTypeDTO> page(DynMap params) {
        IPage<SysDictTypeEntity> page = baseDao.selectPage(
            getPage(params, "sort", true),
            getWrapper(params)
        );

        return getPageData(page, SysDictTypeDTO.class);
    }

    private QueryWrapper<SysDictTypeEntity> getWrapper(DynMap params){
        String dictType = (String) params.get("dictType");
        String dictName = (String) params.get("dictName");

        QueryWrapper<SysDictTypeEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(dictType), "dict_type", dictType);
        wrapper.like(StringUtils.isNotBlank(dictName), "dict_name", dictName);

        return wrapper;
    }


    public PageData<SysDictTypeDTO> getPage(DynMap params) {
        IPage<SysDictTypeEntity> page = baseDao.selectPage(
                getPage(params, "sort", true),
                getWrapper(params)
        );
        return getPageData(page, SysDictTypeDTO.class);
    }


    @Override
    public SysDictTypeDTO get(Long id) {
        SysDictTypeEntity entity = baseDao.selectById(id);

        return ConvertUtils.sourceToTarget(entity, SysDictTypeDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysDictTypeDTO dto) {
        SysDictTypeEntity entity = ConvertUtils.sourceToTarget(dto, SysDictTypeEntity.class);

        insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictTypeDTO dto) {
        SysDictTypeEntity entity = ConvertUtils.sourceToTarget(dto, SysDictTypeEntity.class);

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        //删除
        deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public List<DictType> getAllList() {
        List<DictType> typeList = baseDao.getDictTypeList();
        List<DictData> dataList = sysDictDataDao.getDictDataList(null);
        for(DictType type : typeList){
            for(DictData data : dataList){
                if(type.getId().equals(data.getDictTypeId())){
                    type.getDataList().add(data);
                }
            }
        }
        return typeList;
    }

    @Override
    public List<DictType> getDictTypeList() {
        return baseDao.getDictTypeList();
    }

    @Override
    public List<LabelDTO> getDictList(String dictType) {
        if (StringUtils.isBlank(dictType)) {
            return List.of();
        }
        List<DictData> dataList = sysDictDataDao.getDictDataList(dictType);
        return dataList.stream().map(this::toLabel).toList();
    }

    private LabelDTO toLabel(DictData data) {
        LabelDTO label = new LabelDTO(data.getDictValue(), data.getDictLabel(), data.getI18nKey());
        label.setAttrType(data.getAttrType());
        return label;
    }
}
