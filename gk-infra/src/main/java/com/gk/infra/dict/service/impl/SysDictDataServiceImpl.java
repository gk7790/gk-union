package com.gk.infra.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.page.PageData;
import com.gk.common.tools.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.dict.dao.SysDictDataDao;
import com.gk.infra.dict.dto.SysDictDataDTO;
import com.gk.infra.dict.entity.SysDictDataEntity;
import com.gk.infra.dict.service.SysDictDataService;
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
public class SysDictDataServiceImpl extends BaseServiceImpl<SysDictDataDao, SysDictDataEntity> implements SysDictDataService {
    // private final SysLanguageService sysLanguageService;

    @Override
    public PageData<SysDictDataDTO> page(DynMap params) {
        IPage<SysDictDataEntity> page = baseDao.selectPage(
            getPage(params, "sort", true),
            getWrapper(params)
        );

        return getPageData(page, SysDictDataDTO.class);
    }

    private QueryWrapper<SysDictDataEntity> getWrapper(DynMap params){
        String dictTypeId = (String) params.get("dictTypeId");
        String dictLabel = (String) params.get("dictLabel");
        String dictValue = (String) params.get("dictValue");

        QueryWrapper<SysDictDataEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_type_id", dictTypeId);
        wrapper.like(StringUtils.isNotBlank(dictLabel), "dict_label", dictLabel);
        wrapper.like(StringUtils.isNotBlank(dictValue), "dict_value", dictValue);

        return wrapper;
    }


    @Override
    public PageData<SysDictDataDTO> getPage(DynMap params) {
        //分页
        IPage<SysDictDataEntity> page = getPage(params, "d.sort", true);
        params.put("language", ReqContextHolder.getLang());
        List<SysDictDataEntity> list = baseDao.getList(params);
        //查询
        return getPageData(list, page.getTotal(), SysDictDataDTO.class);
    }


    @Override
    public SysDictDataDTO get(Long id) {
        SysDictDataEntity entity = baseDao.selectById(id);
//        SysLanguageEntity language = sysLanguageService.getLanguage("sys_dict_data", id, "label", HttpContextUtils.getLanguage());
//        String label = Optional.ofNullable(language).map(SysLanguageEntity::getFieldValue).orElse("");
//        if (StringUtils.isNotEmpty(label)) {
//            entity.setDictLabel(label);
//        }
        return ConvertUtils.sourceToTarget(entity, SysDictDataDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysDictDataDTO dto) {
        SysDictDataEntity entity = ConvertUtils.sourceToTarget(dto, SysDictDataEntity.class);
        insert(entity);
//        saveLanguage(entity.getId(), "label", dto.getDictLabel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictDataDTO dto) {
        SysDictDataEntity entity = ConvertUtils.sourceToTarget(dto, SysDictDataEntity.class);

        updateById(entity);
//        saveLanguage(entity.getId(), "label", dto.getDictLabel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        //删除
        deleteBatchIds(Arrays.asList(ids));
//        for (Long id : ids) {
//            //删除菜单国际化
//            sysLanguageService.deleteLanguage("sys_dict_data", id);
//        }
    }

//    private void saveLanguage(Long tableId, String fieldName, String fieldValue){
//        sysLanguageService.saveOrUpdate("sys_dict_data", tableId, fieldName, fieldValue, HttpContextUtils.getLanguage());
//    }

}