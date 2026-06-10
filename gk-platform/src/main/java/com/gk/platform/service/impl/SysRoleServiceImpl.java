package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.platform.dao.SysRoleDao;
import com.gk.platform.dto.SysRoleDTO;
import com.gk.platform.entity.SysRoleEntity;
import com.gk.platform.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 角色
 * 
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {
	private final SysRoleMenuService sysRoleMenuService;
	private final SysRoleDataScopeService sysRoleDataScopeService;
	private final SysUserSubjectService sysUserSubjectService;
	private final SysDeptService sysDeptService;

    @Override
	public PageData<SysRoleDTO> page(DynMap params) {
		IPage<SysRoleEntity> page = baseDao.selectPage(
			getPage(params, Constant.CREATED_AT, false),
			getWrapper(params)
		);

		return getPageData(page, SysRoleDTO.class);
	}

	@Override
	public List<SysRoleDTO> list(DynMap params) {
		List<SysRoleEntity> entityList = baseDao.selectList(getWrapper(params));

		return ConvertUtils.sourceToTarget(entityList, SysRoleDTO.class);
	}

	private QueryWrapper<SysRoleEntity> getWrapper(DynMap params){
		String name = (String)params.get("name");
		String roleScope = params.getStr("roleScope");
		Long tenantId = params.getLong("tenantId", null);
		List<Integer> statusList = params.getList("status", Integer.class, null);
		Boolean templateOnly = params.getBool("templateOnly", false);

		QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
		wrapper.like(StringUtils.isNotBlank(name), "name", name);
		wrapper.eq(StringUtils.isNotBlank(roleScope), "role_scope", roleScope);
		wrapper.eq(tenantId != null, "tenant_id", tenantId);
		wrapper.isNull(Boolean.TRUE.equals(templateOnly), "tenant_id");
		wrapper.in(statusList != null && !statusList.isEmpty(), "status", statusList);

		//普通管理员，只能查询所属部门及子部门的数据
		if(!ReqContextHolder.isSAdmin()) {
			wrapper.eq("tenant_id", ReqContextHolder.getTenantId());
		}

		return wrapper;
	}

	@Override
	public SysRoleDTO get(Long id) {
		SysRoleEntity entity = baseDao.selectById(id);

		return ConvertUtils.sourceToTarget(entity, SysRoleDTO.class);
	}

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        List<Integer> list = params.getList("status", Integer.class, StatusEnum.defaultStatus());

        QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name");
		String roleScope = params.getStr("roleScope");
		Long tenantId = params.getLong("tenantId", null);
		Boolean templateOnly = params.getBool("templateOnly", false);
		wrapper.eq(StringUtils.isNotBlank(roleScope), "role_scope", roleScope);
		wrapper.eq(tenantId != null, "tenant_id", tenantId);
		wrapper.isNull(Boolean.TRUE.equals(templateOnly), "tenant_id");
        //普通管理员，只能查询所属租户数据
        if(!ReqContextHolder.isSAdmin()) {
            wrapper.eq("tenant_id", ReqContextHolder.getTenantId());
        }
        wrapper.in("status", list);
        List<SysRoleEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }

    @Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysRoleDTO dto) {
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);

		//保存角色
		insert(entity);
		dto.setId(entity.getId());

		//保存角色菜单关系
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());

		//保存角色数据权限关系
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysRoleDTO dto) {
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);

		//更新角色
		updateById(entity);

		//更新角色菜单关系
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());

		//更新角色数据权限关系
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long[] ids) {
		//删除角色
		baseDao.deleteBatchIds(Arrays.asList(ids));

		//删除用户主体角色关系
		sysUserSubjectService.deleteByRoleIds(ids);

		//删除角色菜单关系
		sysRoleMenuService.deleteByRoleIds(ids);

		//删除角色数据权限关系
		sysRoleDataScopeService.deleteByRoleIds(ids);
	}

}