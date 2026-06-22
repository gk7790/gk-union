package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.service.PspAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PspAccountServiceImpl extends CrudServiceImpl<PspAccountDao, PspAccountEntity, PspAccountDTO> implements PspAccountService {
    @Autowired
    private PayinPlanCache payinPlanCache;
    @Autowired
    private PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public QueryWrapper<PspAccountEntity> getWrapper(DynMap params) {
        QueryWrapper<PspAccountEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspAccountNo = params.getStr("pspAccountNo");
        String pspAccountName = params.getStr("pspAccountName");
        String secretType = params.getStr("secretType");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspAccountNo), "psp_account_no", pspAccountNo);
        wrapper.like(StrUtil.isNotBlank(pspAccountName), "psp_account_name", pspAccountName);
        wrapper.eq(StrUtil.isNotBlank(secretType), "secret_type", secretType);
        return wrapper;
    }

    @Override
    public void save(PspAccountDTO dto) {
        super.save(dto);
        evictPayinPlanCache();
    }

    @Override
    public void update(PspAccountDTO dto) {
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
        // PSP Account 配置会影响路由账号和密钥，变更后必须清空 PayinPlan 缓存。
        if (payinPlanCache != null) {
            payinPlanCache.evictAll();
        }
        if (paymentPlanCacheService != null) {
            paymentPlanCacheService.evictAll();
        }
    }
}
