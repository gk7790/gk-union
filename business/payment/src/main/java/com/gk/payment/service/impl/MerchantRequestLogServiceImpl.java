package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.MerchantRequestLogDao;
import com.gk.payment.dto.MerchantRequestLogDTO;
import com.gk.payment.entity.MerchantRequestLogEntity;
import com.gk.payment.service.MerchantRequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MerchantRequestLogServiceImpl extends CrudServiceImpl<MerchantRequestLogDao, MerchantRequestLogEntity, MerchantRequestLogDTO> implements MerchantRequestLogService {

    @Override
    public QueryWrapper<MerchantRequestLogEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantRequestLogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        String requestNo = params.getStr("requestNo");
        String appId = params.getStr("appId");
        String apiPath = params.getStr("apiPath");
        String apiName = params.getStr("apiName");
        String clientIp = params.getStr("clientIp");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String merchantOrderNo = params.getStr("merchantOrderNo");
        String responseCode = params.getStr("responseCode");
        String status = params.getStr("status");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(StrUtil.isNotBlank(requestNo), "request_no", requestNo);
        wrapper.eq(StrUtil.isNotBlank(appId), "app_id", appId);
        wrapper.eq(StrUtil.isNotBlank(apiPath), "api_path", apiPath);
        wrapper.like(StrUtil.isNotBlank(apiName), "api_name", apiName);
        wrapper.eq(StrUtil.isNotBlank(clientIp), "client_ip", clientIp);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(merchantOrderNo), "merchant_order_no", merchantOrderNo);
        wrapper.eq(StrUtil.isNotBlank(responseCode), "response_code", responseCode);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }

    @Override
    public void record(MerchantRequestLogEntity entity) {
        try {
            baseDao.insert(entity);
        } catch (Exception ex) {
            log.warn("Save merchant request log failed: {}", ex.getMessage());
        }
    }
}
