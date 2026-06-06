package com.gk.platform.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.platform.dto.SysRoleDTO;
import com.gk.platform.entity.SysRoleEntity;

import java.util.List;


/**
 * 角色
 * 
 * @author Lowen
 */
public interface SysRoleService extends BaseService<SysRoleEntity> {

	PageData<SysRoleDTO> page(DynMap params);

	List<SysRoleDTO> list(DynMap params);

	SysRoleDTO get(Long id);

	void save(SysRoleDTO dto);

	void update(SysRoleDTO dto);

	void delete(Long[] ids);

    List<LabelDTO> getDict(DynMap params);
}