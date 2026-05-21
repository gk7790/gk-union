package com.gk.meta.service.impl;

import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.AuthUser;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.TreeUtils;
import com.gk.meta.dao.SysMenuDao;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.entity.SysMenuEntity;
import com.gk.meta.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {
    private final CurrentUser currentUser;
    // private final SysRoleMenuService sysRoleMenuService;
//    private final SysLanguageService sysLanguageService;
    private final RedisUtils redisUtils;

    @Override
	public SysMenuDTO get(Long id) {
		SysMenuEntity entity = baseDao.getById(id, ReqContextHolder.getLang());

		SysMenuDTO dto = ConvertUtils.sourceToTarget(entity, SysMenuDTO.class);

		return dto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addMenu(SysMenuEntity entity) {
        entity.getMeta().setOrder(entity.getSort());
		//保存菜单
		insert(entity);
		// saveLanguage(entity.getId(), "meta.tile", ValueUtils.defaultValue(entity.getMeta().getTitle(), entity.getName()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysMenuDTO dto) {
        SysMenuEntity entity = ConvertUtils.sourceToTarget(dto, SysMenuEntity.class);

		//上级菜单不能为自身
		if(entity.getId().equals(entity.getPid())){
			throw new GkException(ErrorCode.SUPERIOR_MENU_ERROR);
		}
        entity.getMeta().setOrder(entity.getSort());
		//更新菜单
		updateById(entity);
		// saveLanguage(entity.getId(), "meta.tile", ValueUtils.defaultValue(entity.getMeta().getTitle(), entity.getName()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		//删除菜单
		deleteById(id);

		//删除菜单国际化
//		sysLanguageService.deleteLanguage("sys_menu", id);

		// TODO 删除角色菜单关系
		// sysRoleMenuService.deleteByMenuId(id);
	}

	@Override
	public List<SysMenuDTO> getAllMenuList(List<Integer> typeList) {
		List<SysMenuEntity> menuList = baseDao.getMenuList(typeList, ReqContextHolder.getLang());
        List<SysMenuDTO> dtoList = ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class);
        return TreeUtils.build(dtoList, Constant.MENU_ROOT);
	}

	@Override
	public List<SysMenuDTO> getUserMenuList(AuthUser user, List<Integer> typeList) {
		List<SysMenuEntity> menuList;

		//系统管理员，拥有最高权限
		if(user.isSAdmin()){
			menuList = baseDao.getMenuList(typeList, ReqContextHolder.getLang());
		}else {
			menuList = baseDao.getUserMenuList(user.getId(), typeList, ReqContextHolder.getLang());
		}

		List<SysMenuDTO> dtoList = ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class);

		return TreeUtils.build(dtoList);
	}

	@Override
	public List<SysMenuDTO> getListPid(Long pid) {
		List<SysMenuEntity> menuList = baseDao.getListPid(pid);

		return ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class);
	}

//	private void saveLanguage(Long tableId, String fieldName, String fieldValue){
//		sysLanguageService.saveOrUpdate("sys_menu", tableId, fieldName, fieldValue, HttpContextUtils.getLanguage());
//	}

}