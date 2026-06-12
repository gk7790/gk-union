package com.gk.platform.service.impl;


import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.TreeUtils;
import com.gk.platform.dao.SysDeptDao;
import com.gk.platform.dao.SysUserDao;
import com.gk.platform.dto.SysDeptDTO;
import com.gk.platform.entity.SysDeptEntity;
import com.gk.platform.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptDao, SysDeptEntity> implements SysDeptService {
    private final SysUserDao sysUserDao;
    private final RedisUtils redisUtils;

    @Override
	public List<SysDeptDTO> list(Map<String, Object> params) {
		//普通管理员，只能查询所属部门及子部门的数据
		if(!ReqContextHolder.isSuperAdmin()) {
			params.put("deptIdList", ReqContextHolder.getSubDeptIds());
		}

		//查询部门列表
		List<SysDeptEntity> entityList = baseDao.getList(params);

		List<SysDeptDTO> dtoList = ConvertUtils.sourceToTarget(entityList, SysDeptDTO.class);

		return TreeUtils.build(dtoList);
	}

	@Override
	public SysDeptDTO get(Long id) {
		//超级管理员，部门ID为null
		if(id == null){
			return null;
		}

		SysDeptEntity entity = baseDao.getById(id);

		return ConvertUtils.sourceToTarget(entity, SysDeptDTO.class);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysDeptDTO dto) {
		SysDeptEntity entity = ConvertUtils.sourceToTarget(dto, SysDeptEntity.class);
        if (!ReqContextHolder.isSuperAdmin()) {
            entity.setTenantId(ReqContextHolder.getTenantId());
            // 清缓存
            clearCache(entity.getId());
        }
		entity.setPids(getPidList(entity.getPid()));
		insert(entity);
		dto.setId(entity.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysDeptDTO dto) {
		SysDeptEntity entity = ConvertUtils.sourceToTarget(dto, SysDeptEntity.class);

		//上级部门不能为自身
		if(entity.getId().equals(entity.getPid())){
			throw new GkException(ErrorCode.SUPERIOR_DEPT_ERROR);
		}

		// 用户只能修改下级部门
		Set<Long> subDeptList = ReqContextHolder.getSubDeptIds();
		if(!subDeptList.contains(entity.getId())){
			throw new GkException(ErrorCode.SUPERIOR_DEPT_ERROR);
		}
        if (!ReqContextHolder.isSuperAdmin()) {
            // 清缓存
            clearCache(entity.getId());
        }
		entity.setPids(getPidList(entity.getPid()));
		updateById(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		//判断是否有子部门
		Set<Long> subList = ReqContextHolder.getSubDeptIds();
		if(subList.size() > 1){
			throw new GkException(ErrorCode.DEPT_SUB_DELETE_ERROR);
		}

		//判断部门下面是否有用户
		int count = sysUserDao.getCountByDeptId(id);
		if(count > 0){
			throw new GkException(ErrorCode.DEPT_USER_DELETE_ERROR);
		}
        clearCache(id);
		//删除
		baseDao.deleteById(id);
	}

	/**
	 * 获取所有上级部门ID
	 * @param pid 上级ID
	 */
	private String getPidList(Long pid){
		//顶级部门，无上级部门
		if(Constant.DEPT_ROOT.equals(pid)){
			return Constant.DEPT_ROOT + "";
		}

		//所有部门的id、pid列表
		List<SysDeptEntity> deptList = baseDao.getIdAndPidList();

		//list转map
		Map<Long, SysDeptEntity> map = new HashMap<>(deptList.size());
		for(SysDeptEntity entity : deptList){
			map.put(entity.getId(), entity);
		}

		//递归查询所有上级部门ID列表
		List<Long> pidList = new ArrayList<>();
		getPidTree(pid, map, pidList);

		return StringUtils.join(pidList, ",");
	}

	private void getPidTree(Long pid, Map<Long, SysDeptEntity> map, List<Long> pidList) {
		//顶级部门，无上级部门
		if(Constant.DEPT_ROOT.equals(pid)){
			return ;
		}

		//上级部门存在
		SysDeptEntity parent = map.get(pid);
		if(parent != null){
			getPidTree(parent.getPid(), map, pidList);
		}

		pidList.add(pid);
	}

    public void clearCache(Long deptId) {
        String deptIdsKey = RedisKeys.getDeptIdsKey(deptId);
        String deptIdsKey1 = RedisKeys.getDeptIdsKey(ReqContextHolder.getDeptId());
        // 清缓存
        redisUtils.delete(List.of(deptIdsKey, deptIdsKey1));
    }
}