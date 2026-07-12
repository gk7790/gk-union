package com.gk.meta.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.TreeUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysMenuDao;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.entity.SysMenuEntity;
import com.gk.meta.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
	public List<SysMenuDTO> getNavMenuList(List<Integer> typeList) {
        List<SysMenuEntity> menuList = loadNavMenus(typeList);
        menuList = filterEnabledNavMenus(menuList);
        stripInternalFields(menuList);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getAdminMenuList(List<Integer> typeList) {
        String subjectType = ReqContextHolder.isSuperAdmin() ? null : ReqContextHolder.getSubjectType();
        List<SysMenuEntity> menuList = baseDao.getCatalogMenuList(typeList, subjectType);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getRoleSelectMenuList(String roleScope, List<Integer> typeList) {
        AssertUtils.isBlank(roleScope, "roleScope");
        List<SysMenuEntity> menuList = ReqContextHolder.isSuperAdmin()
                ? baseDao.getCatalogMenuList(typeList, roleScope)
                : loadAuthorizedMenus(roleScope, typeList);
        stripInternalFields(menuList);
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

    private List<SysMenuEntity> loadNavMenus(List<Integer> typeList) {
        if (ReqContextHolder.isSuperAdmin()) {
            return baseDao.getNavCatalogMenuList(typeList, null, StatusEnum.NORMAL.code());
        }
        return loadAuthorizedMenus(ReqContextHolder.getSubjectType(), typeList);
    }

    private List<SysMenuEntity> loadAuthorizedMenus(String subjectType, List<Integer> typeList) {
        Long userSubjectId = ReqContextHolder.getSubjectId();
        if (userSubjectId == null) {
            throw new GkException(ErrorCode.UNAUTHORIZED);
        }
        return baseDao.getNavMenuList(userSubjectId, subjectType, typeList, StatusEnum.NORMAL.code());
    }

    private List<SysMenuEntity> filterEnabledNavMenus(List<SysMenuEntity> menuList) {
        if (CollectionUtils.isEmpty(menuList)) {
            return menuList;
        }
        Set<Long> visibleIds = menuList.stream()
                .map(SysMenuEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> hiddenIds = new HashSet<>();
        for (SysMenuEntity menu : menuList) {
            if (menu.getId() == null) {
                continue;
            }
            if (!StatusEnum.NORMAL.code().equals(menu.getStatus()) || parentHidden(menu, visibleIds)) {
                hiddenIds.add(menu.getId());
            }
        }
        if (hiddenIds.isEmpty()) {
            return menuList;
        }
        boolean changed;
        do {
            changed = false;
            for (SysMenuEntity menu : menuList) {
                if (menu.getId() != null && !hiddenIds.contains(menu.getId()) && hiddenIds.contains(menu.getPid())) {
                    hiddenIds.add(menu.getId());
                    changed = true;
                }
            }
        } while (changed);
        return menuList.stream()
                .filter(menu -> menu.getId() == null || !hiddenIds.contains(menu.getId()))
                .toList();
    }

    private boolean parentHidden(SysMenuEntity menu, Set<Long> visibleIds) {
        Long pid = menu.getPid();
        return pid != null && pid != 0 && !visibleIds.contains(pid);
    }

    private void stripInternalFields(List<SysMenuEntity> menuList) {
        for (SysMenuEntity menu : menuList) {
            menu.setDomain(null);
        }
    }

    private void assertSubjectTypes(List<String> subjectTypes) {
        if (CollectionUtils.isEmpty(subjectTypes)) {
            throw new GkException(ErrorCode.BAD_REQUEST, "subjectTypes");
        }
    }
}
