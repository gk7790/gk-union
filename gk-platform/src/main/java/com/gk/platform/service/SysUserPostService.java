
package com.gk.platform.service;

import com.gk.common.core.service.BaseService;
import com.gk.platform.entity.SysUserPostEntity;

import java.util.List;

/**
 * 用户岗位关系
 *
 * @author Lowen
 */
public interface SysUserPostService extends BaseService<SysUserPostEntity> {

    /**
     * 保存或修改
     * @param userId      用户ID
     * @param postIdList  岗位ID列表
     */
    void saveOrUpdate(Long userId, List<Long> postIdList);

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