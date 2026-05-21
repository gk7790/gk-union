
package com.gk.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.core.dao.BaseDao;
import com.gk.platform.entity.SysUserPostEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* 用户岗位关系
*
* @author Mark sunlightcs@gmail.com
*/
@Mapper
public interface SysUserPostDao extends BaseMapper<SysUserPostEntity> {

    /**
     * 根据岗位ids，删除岗位用户关系
     * @param postIds 岗位ids
     */
    void deleteByPostIds(Long[] postIds);

    /**
     * 根据用户id，删除岗位用户关系
     * @param userIds 用户ids
     */
    void deleteByUserIds(Long[] userIds);

    /**
     * 岗位ID列表
     * @param userId  用户ID
     */
    List<Long> getPostIdList(Long userId);
}