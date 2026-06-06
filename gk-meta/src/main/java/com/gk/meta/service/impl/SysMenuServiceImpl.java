package com.gk.meta.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.AuthUser;
import com.gk.common.enums.MenuTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.TreeUtils;
import com.gk.infra.enums.ScopeEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysMenuDao;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.dto.SysMenuMeta;
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
    private final RedisUtils redisUtils;

    @Override
	public SysMenuDTO get(Long id) {
		SysMenuEntity entity = baseDao.getById(id);
		SysMenuDTO dto = ConvertUtils.sourceToTarget(entity, SysMenuDTO.class);
		return dto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addMenu(SysMenuEntity entity) {
        entity.getMeta().setOrder(entity.getSort());
		//保存菜单
		insert(entity);
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
	public List<SysMenuDTO> getUserMenuList(List<Integer> typeList, long minId) {
        Long userId = ReqContextHolder.getUserId();
        List<SysMenuEntity> menuList;
        //系统管理员，拥有最高权限
		if(ReqContextHolder.isSAdmin()){
			menuList = baseDao.getMenuList(typeList,0, 0);
		} else if (ScopeEnum.sysList().contains(ReqContextHolder.getScope())) {
            menuList = baseDao.getUserMenuList(userId, typeList, ReqContextHolder.getScope(), 0, minId);
        } else {
            menuList = baseDao.getUserMenuList(userId, typeList, ReqContextHolder.getScope(), ReqContextHolder.getDomain(), minId);
        }
        for (SysMenuEntity sysMenu : menuList) {
            sysMenu.setScope(null);
            sysMenu.setDomain(null);
        }
		List<SysMenuDTO> dtoList = ConvertUtils.sourceToTarget(menuList, SysMenuDTO.class);
		return TreeUtils.build(dtoList);
	}

    public SysMenuDTO defaultNav(){
        SysMenuDTO dto = new SysMenuDTO();
        dto.setId(1L);
        dto.setPid(0L);
        dto.setName("Dashboard");
        dto.setPath("/analytics");
        dto.setSort(-1);
        dto.setStatus(StatusEnum.NORMAL.code());
        dto.setComponent("/dashboard/analytics/index");
        dto.setType(MenuTypeEnum.MENU.code());
        SysMenuMeta meta  = new SysMenuMeta();
        meta.setAffixTab(true);
        meta.setIcon("carbon:workspace");
        meta.setTitle("menu.dashboard");
        meta.setOrder(-1);
        dto.setMeta(meta);
        return dto;
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

}