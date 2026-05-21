package com.gk.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.core.dao.BaseDao;
import com.gk.platform.entity.SysPostEntity;
import org.apache.ibatis.annotations.Mapper;

/**
* 岗位管理
*
* @author Mark sunlightcs@gmail.com
*/
@Mapper
public interface SysPostDao extends BaseMapper<SysPostEntity> {
	
}