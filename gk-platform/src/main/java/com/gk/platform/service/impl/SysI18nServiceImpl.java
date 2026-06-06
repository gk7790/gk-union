package com.gk.platform.service.impl;

import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.Result;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.config.service.SysParamsService;
import com.gk.infra.i18n.dao.SysI18nDao;
import com.gk.infra.i18n.entity.SysI18nEntity;
import com.gk.platform.dto.SysI18nDTO;
import com.gk.platform.service.SysI18nService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 系统-国际化
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class SysI18nServiceImpl extends CrudServiceImpl<SysI18nDao, SysI18nEntity, SysI18nDTO> implements SysI18nService {
    private final SysParamsService sysParamsService;

    @Override
    public QueryWrapper<SysI18nEntity> getWrapper(DynMap params){
        QueryWrapper<SysI18nEntity> wrapper = new QueryWrapper<>();

        return wrapper;
    }


    @Override
    public PageData<SysI18nDTO> getPage(DynMap params) {
        IPage<SysI18nEntity> page = getPage(params, Constant.CREATED_AT, false);
        List<SysI18nEntity> list = baseDao.getPageList(params);
        return getPageData(list, page.getTotal(), currentDtoClass());
    }

    @Override
    public List<SysI18nDTO> getList(DynMap params) {
        List<DynMap> langList = sysParamsService.getValueList(Constant.SYS_I18N_PARAMS_KEY, DynMap.class);
        List<SysI18nEntity> list = baseDao.getList(params);

        // 当前已有语言
        Set<String> existsLangSet = list.stream()
                .map(SysI18nEntity::getLang)
                .collect(Collectors.toSet());

        // 取一个基础对象（复制公共字段）
        SysI18nEntity base = list.isEmpty()
                ? new SysI18nEntity()
                : list.getFirst();

        // 自动补全缺失语言
        for (DynMap langItem : langList) {
            String value = langItem.getStr("value");
            if (!existsLangSet.contains(value)) {
                SysI18nEntity entity = new SysI18nEntity();
                // 复制公共字段
                entity.setTenantId(base.getTenantId());
                entity.setBizType(base.getBizType());
                entity.setI18nKey(base.getI18nKey());
                // 设置语言
                entity.setLang(value);
                // 默认空值
                entity.setValue("");
                list.add(entity);
            }
        }
        return ConvertUtils.sourceToTarget(list, SysI18nDTO.class);
    }

    @Override
    public Result<String> addKeyValue(SysI18nDTO dto) {
        Long tenantId = ReqContextHolder.getTenantId();
        if (MapUtil.isNotEmpty(dto.getItems())) {
            for (Map.Entry<String, String> entry : dto.getItems().entrySet()) {
                SysI18nEntity entity = new SysI18nEntity();
                // 复制公共字段
                entity.setTenantId(tenantId);
                entity.setBizType(dto.getBizType());
                entity.setI18nKey(dto.getI18nKey());
                // 设置语言
                entity.setLang(entry.getKey());
                // 默认空值
                entity.setValue(entry.getValue());
                baseDao.insert(entity);
            }
        }
        return Result.success("success");
    }
}