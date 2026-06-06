package com.gk.infra.config.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.beans.SysParamsRedis;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.config.dao.SysParamsDao;
import com.gk.infra.config.dto.SysParamsDTO;
import com.gk.infra.config.entity.SysParamsEntity;
import com.gk.infra.config.service.SysParamsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysParamsServiceImpl extends BaseServiceImpl<SysParamsDao, SysParamsEntity> implements SysParamsService {
    private final SysParamsRedis sysParamsRedis;

    private QueryWrapper<SysParamsEntity> getWrapper(DynMap params) {
        String paramCode = (String) params.get("paramCode");
        QueryWrapper<SysParamsEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("param_type", 1);
        wrapper.like(StringUtils.isNotBlank(paramCode), "param_code", paramCode);
        return wrapper;
    }

    @Override
    public PageData<SysParamsDTO> page(DynMap params) {
        IPage<SysParamsEntity> page = baseDao.selectPage(
                getPage(params, Constant.CREATED_AT, false),
                getWrapper(params)
        );
        return getPageData(page, SysParamsDTO.class);
    }

    @Override
    public List<SysParamsDTO> list(DynMap params) {
        List<SysParamsEntity> entityList = baseDao.selectList(getWrapper(params));

        return ConvertUtils.sourceToTarget(entityList, SysParamsDTO.class);
    }

    @Override
    public SysParamsDTO get(Long id) {
        SysParamsEntity entity = baseDao.selectById(id);

        return ConvertUtils.sourceToTarget(entity, SysParamsDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysParamsDTO dto) {
        SysParamsEntity entity = ConvertUtils.sourceToTarget(dto, SysParamsEntity.class);
        insert(entity);

        sysParamsRedis.set(entity.getParamCode(), entity.getParamValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysParamsDTO dto) {
        SysParamsEntity entity = ConvertUtils.sourceToTarget(dto, SysParamsEntity.class);
        updateById(entity);

        sysParamsRedis.set(entity.getParamCode(), entity.getParamValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        //删除Redis数据
        List<String> paramCodeList = baseDao.getParamCodeList(ids);
        for (String key : paramCodeList) {
            sysParamsRedis.delete(key);
        }
        //删除
        deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public String getValue(String paramCode) {
        String paramValue = sysParamsRedis.get(paramCode);
        if (paramValue == null) {
            paramValue = baseDao.getValueByCode(paramCode);

            sysParamsRedis.set(paramCode, paramValue);
        }
        return paramValue;
    }

    @Override
    public <T> T getValueObject(String paramCode, Class<T> clazz) {
        String paramValue = getValue(paramCode);
        if (StringUtils.isNotBlank(paramValue)) {
            return JSONObject.parseObject(paramValue, clazz);
        }

        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new GkException(ErrorCode.PARAMS_GET_ERROR);
        }
    }

    @Override
    public <T> List<T> getValueList(String paramCode, Class<T> clazz) {
        String paramValue = getValue(paramCode);
        if (StringUtils.isBlank(paramValue)) {
            return List.of();
        }

        try {
            return JSONArray.parseArray(paramValue, clazz);
        } catch (Exception e) {
            throw new GkException(ErrorCode.PARAMS_GET_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateValueByCode(String paramCode, String paramValue) {
        SysParamsEntity entity = this.selectByParamCode(paramCode);
        int count;
        if (entity == null) {
            entity = new SysParamsEntity();
            entity.setParamCode(paramCode);
            entity.setParamValue(paramValue);
            entity.setRemark("remark");
            entity.setParamType(1);
            count = baseDao.insert(entity);
        } else {
            count = baseDao.updateValueByCode(paramCode, paramValue);
        }
        sysParamsRedis.set(paramCode, paramValue);
        return count;
    }

    @Override
    public SysParamsEntity selectByParamCode(String paramCode) {
        QueryWrapper<SysParamsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("param_code", paramCode);
        return baseDao.selectOne(queryWrapper);
    }

}