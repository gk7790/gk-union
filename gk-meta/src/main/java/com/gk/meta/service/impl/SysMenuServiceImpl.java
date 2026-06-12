package com.gk.meta.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.TreeUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.meta.dao.SysMenuDao;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.entity.SysMenuEntity;
import com.gk.meta.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {

    @Override
	public SysMenuDTO get(Long id) {
		SysMenuEntity entity = baseDao.getById(id);
		return ConvertUtils.sourceToTarget(entity, SysMenuDTO.class);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addMenu(SysMenuEntity entity) {
        assertSubjectTypes(entity.getSubjectTypes());
        entity.getMeta().setOrder(entity.getSort());
		insert(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysMenuDTO dto) {
        SysMenuEntity entity = ConvertUtils.sourceToTarget(dto, SysMenuEntity.class);
        assertSubjectTypes(entity.getSubjectTypes());

		if (entity.getId().equals(entity.getPid())) {
			throw new GkException(ErrorCode.SUPERIOR_MENU_ERROR);
		}
        entity.getMeta().setOrder(entity.getSort());
		updateById(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		deleteById(id);
	}

	@Override
	public List<SysMenuDTO> getNavMenuList(List<Integer> typeList, long minId) {
        List<SysMenuEntity> menuList = loadNavMenus(typeList, minId);
        stripInternalFields(menuList);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getAdminMenuList(List<Integer> typeList, long minId) {
        String subjectType = ReqContextHolder.isSAdmin() ? null : ReqContextHolder.getSubjectType();
        List<SysMenuEntity> menuList = baseDao.getCatalogMenuList(typeList, subjectType, minId);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getRoleSelectMenuList(String roleScope, List<Integer> typeList, long minId) {
        AssertUtils.isBlank(roleScope, "roleScope");
        List<SysMenuEntity> menuList = baseDao.getCatalogMenuList(typeList, roleScope, minId);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public void assertMenusMatchRoleScope(String roleScope, List<Long> menuIdList) {
        if (CollectionUtils.isEmpty(menuIdList) || StringUtils.isBlank(roleScope)) {
            return;
        }
        long mismatch = baseDao.countMenusNotInSubjectType(menuIdList, roleScope);
        if (mismatch > 0) {
            throw new GkException(ErrorCode.ROLE_MENU_SUBJECT_MISMATCH);
        }
	}

	@Override
	public List<SysMenuDTO> getListPid(Long pid) {
		List<SysMenuEntity> menuList = baseDao.getListPid(pid);
		return ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class);
	}

    @Override
    public boolean isExistsName(Long id, String name) {
        QueryWrapper<SysMenuEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        wrapper.ne("id", id);
        return baseDao.exists(wrapper);
    }

    private List<SysMenuEntity> loadNavMenus(List<Integer> typeList, long minId) {
        if (ReqContextHolder.isSAdmin()) {
            return baseDao.getCatalogMenuList(typeList, ReqContextHolder.getSubjectType(), minId);
        }
        Long userId = ReqContextHolder.getUserId();
        Long roleId = ReqContextHolder.getRoleId();
        if (userId == null || roleId == null) {
            throw new GkException(ErrorCode.UNAUTHORIZED);
        }
        return baseDao.getNavMenuList(userId, roleId, ReqContextHolder.getSubjectType(), typeList, minId);
    }

    private void stripInternalFields(List<SysMenuEntity> menuList) {
        for (SysMenuEntity menu : menuList) {
            menu.setSubjectTypes(null);
            menu.setDomain(null);
        }
    }

    private void assertSubjectTypes(List<String> subjectTypes) {
        if (CollectionUtils.isEmpty(subjectTypes)) {
            throw new GkException(ErrorCode.NOT_NULL);
        }
    }
}
