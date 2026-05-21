package com.gk.devtools.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.page.PageData;
import com.gk.devtools.dao.BaseClassDao;
import com.gk.devtools.entity.BaseClassEntity;
import com.gk.devtools.service.BaseClassService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 基类管理
 *
 * @author Lowen
 */
@Service
public class BaseClassServiceImpl extends BaseServiceImpl<BaseClassDao, BaseClassEntity> implements BaseClassService {

    @Override
    public PageData<BaseClassEntity> page(Map<String, Object> params) {
        IPage<BaseClassEntity> page = baseDao.selectPage(
            getPage(params, Constant.CREATED_AT, false),
            getWrapper(params)
        );
        return new PageData<>(page.getRecords(), page.getTotal());
    }

    @Override
    public List<BaseClassEntity> list() {
        return baseDao.selectList(null);
    }

    private QueryWrapper<BaseClassEntity> getWrapper(Map<String, Object> params){
        String code = (String)params.get("code");

        QueryWrapper<BaseClassEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(code), "code", code);

        return wrapper;
    }
}