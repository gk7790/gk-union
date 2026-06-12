package com.gk.meta.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.enums.MenuTypeEnum;
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
        resolveSubjectTypes(entity);
        entity.getMeta().setOrder(entity.getSort());
		insert(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysMenuDTO dto) {
        SysMenuEntity entity = ConvertUtils.sourceToTarget(dto, SysMenuEntity.class);
        resolveSubjectTypes(entity);

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
        stripInternalFields(menuList);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getAdminMenuList(List<Integer> typeList) {
        String subjectType = ReqContextHolder.isSAdmin() ? null : ReqContextHolder.getSubjectType();
        List<SysMenuEntity> menuList = baseDao.getCatalogMenuList(typeList, subjectType);
		return TreeUtils.build(ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class));
	}

	@Override
	public List<SysMenuDTO> getRoleSelectMenuList(String roleScope, List<Integer> typeList) {
        AssertUtils.isBlank(roleScope, "roleScope");
        List<SysMenuEntity> menuList = baseDao.getCatalogMenuList(typeList, roleScope);
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
        if (ReqContextHolder.isSAdmin()) {
            // 超管：全量菜单目录（不过滤 subjectType / role_menu）
            return baseDao.getCatalogMenuList(typeList, null);
        }
        Long userId = ReqContextHolder.getUserId();
        Long roleId = ReqContextHolder.getRoleId();
        if (userId == null || roleId == null) {
            throw new GkException(ErrorCode.UNAUTHORIZED);
        }
        return baseDao.getNavMenuList(userId, roleId, ReqContextHolder.getSubjectType(), typeList);
    }

    private void stripInternalFields(List<SysMenuEntity> menuList) {
        for (SysMenuEntity menu : menuList) {
            menu.setSubjectTypes(null);
            menu.setDomain(null);
        }
    }

    /**
     * 目录/菜单必填 subjectTypes；按钮未填时继承父菜单。
     */
    private void resolveSubjectTypes(SysMenuEntity entity) {
        if (CollectionUtils.isNotEmpty(entity.getSubjectTypes())) {
            return;
        }
        if (MenuTypeEnum.BUTTON.code().equals(entity.getType())) {
            entity.setSubjectTypes(inheritSubjectTypesFromParent(entity.getPid()));
            return;
        }
        throw new GkException(ErrorCode.BAD_REQUEST, "subjectTypes");
    }

    private List<String> inheritSubjectTypesFromParent(Long pid) {
        AssertUtils.isNull(pid, "pid");
        if (Constant.MENU_ROOT.equals(pid)) {
            throw new GkException(ErrorCode.SUPERIOR_MENU_ERROR);
        }
        SysMenuEntity parent = baseDao.getById(pid);
        if (parent == null || CollectionUtils.isEmpty(parent.getSubjectTypes())) {
            throw new GkException(ErrorCode.SUPERIOR_MENU_ERROR);
        }
        return parent.getSubjectTypes();
    }
}
