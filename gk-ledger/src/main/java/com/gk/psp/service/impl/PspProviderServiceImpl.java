package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dto.PspProviderDTO;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.service.PspProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PspProviderServiceImpl extends CrudServiceImpl<PspProviderDao, PspProviderEntity, PspProviderDTO> implements PspProviderService {

    @Override
    public QueryWrapper<PspProviderEntity> getWrapper(DynMap params) {
        QueryWrapper<PspProviderEntity> wrapper = new QueryWrapper<>();
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspCode = params.getStr("pspCode");
        String pspName = params.getStr("pspName");
        String countryCode = params.getStr("countryCode");

        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.like(StrUtil.isNotBlank(pspName), "psp_name", pspName);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        return wrapper;
    }

    @Override
    public void save(PspProviderDTO dto) {
        super.save(dto);
        evictPayinPlanCache();
    }

    @Override
    public void update(PspProviderDTO dto) {
        super.update(dto);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPayinPlanCache();
    }

    private void evictPayinPlanCache() {
        // PSP 模块不直接清理 payment 缓存，后续由模块事件统一处理。
    }
}
