package com.gk.iam.service;


import com.gk.common.core.service.BaseService;
import com.gk.iam.dto.SysDeptDTO;
import com.gk.iam.entity.SysDeptEntity;

import java.util.List;
import java.util.Map;

/**
 * 部门管理
 * 
 * @author Lowen
 */
public interface SysDeptService extends BaseService<SysDeptEntity> {

	List<SysDeptDTO> list(Map<String, Object> params);

	SysDeptDTO get(Long id);

	void save(SysDeptDTO dto);

	void update(SysDeptDTO dto);

	void delete(Long id);

    void clearCache(Long deptId);
}