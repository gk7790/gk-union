package com.gk.platform.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.page.PageData;
import com.gk.common.tools.DynMap;
import com.gk.common.tools.Result;
import com.gk.infra.i18n.entity.SysI18nEntity;
import com.gk.platform.dto.SysI18nDTO;

import java.util.List;

/**
 * 系统-国际化
 *
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-23
 */
public interface SysI18nService extends CrudService<SysI18nEntity, SysI18nDTO> {

    PageData<SysI18nDTO> getPage(DynMap params);

    List<SysI18nDTO> getList(DynMap params);

    Result<String> addKeyValue(SysI18nDTO dto);
}