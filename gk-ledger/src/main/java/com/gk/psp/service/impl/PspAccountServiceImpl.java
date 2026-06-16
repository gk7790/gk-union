package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.service.PspAccountService;
import org.springframework.stereotype.Service;

@Service
public class PspAccountServiceImpl extends CrudServiceImpl<PspAccountDao, PspAccountEntity, PspAccountDTO> implements PspAccountService {

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
}
