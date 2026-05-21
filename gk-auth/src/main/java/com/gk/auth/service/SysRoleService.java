package com.gk.auth.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.auth.dto.SysRoleDTO;
import com.gk.auth.entity.SysRoleEntity;

import java.util.List;


/**
 * 角色
 * 
 * @author Lowen
 */
public interface SysRoleService extends BaseService<SysRoleEntity> {

	PageData<SysRoleDTO> page(DataMap params);

	List<SysRoleDTO> list(DataMap params);

	SysRoleDTO get(Long id);

	void save(SysRoleDTO dto);

	void update(SysRoleDTO dto);

	void delete(Long[] ids);

    List<LabelDTO> getDict(DataMap params);
}