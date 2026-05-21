package com.gk.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.tools.DataMap;
import com.gk.tenant.dao.SysTenantDao;
import com.gk.tenant.dto.SysTenantDTO;
import com.gk.tenant.entity.SysTenantEntity;
import com.gk.tenant.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends CrudServiceImpl<SysTenantDao, SysTenantEntity, SysTenantDTO> implements SysTenantService {

    @Override
    public QueryWrapper<SysTenantEntity> getWrapper(DataMap params) {
        QueryWrapper<SysTenantEntity> wrapper = new QueryWrapper<>();
        return wrapper;
    }

    @Override
    public List<LabelDTO> getDict(DataMap params) {
        List<Integer> list = params.getList("status", Integer.class, Constant.Status.defaultStatus());

        QueryWrapper<SysTenantEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name");
        wrapper.in("status", list);
        List<SysTenantEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }
}