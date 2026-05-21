package com.gk.devtools.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.page.PageData;
import com.gk.devtools.dao.TemplateDao;
import com.gk.devtools.entity.TemplateEntity;
import com.gk.devtools.service.TemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板管理
 *
 * @author Lowen
 */
@Service
public class TemplateServiceImpl extends BaseServiceImpl<TemplateDao, TemplateEntity> implements TemplateService {
    @Override
    public PageData<TemplateEntity> page(Map<String, Object> params) {
        IPage<TemplateEntity> page = baseDao.selectPage(
            getPage(params, Constant.CREATED_AT, false),
            getWrapper(params)
        );
        return new PageData<>(page.getRecords(), page.getTotal());
    }

    private QueryWrapper<TemplateEntity> getWrapper(Map<String, Object> params){
        String name = (String)params.get("name");

        QueryWrapper<TemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(name), "name", name);

        return wrapper;
    }

    @Override
    public List<TemplateEntity> list() {
        QueryWrapper<TemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return baseDao.selectList(wrapper);
    }

    @Override
    public void updateStatusBatch(Long[] ids, int status){
        Map<String, Object> map = new HashMap<>(2);
        map.put("ids", ids);
        map.put("status", status);
        baseDao.updateStatusBatch(map);
    }
}