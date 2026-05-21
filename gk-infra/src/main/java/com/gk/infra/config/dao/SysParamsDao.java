package com.gk.infra.config.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.config.entity.SysParamsEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Mapper
public interface SysParamsDao extends BaseDao<SysParamsEntity> {
    /**
     * 根据参数编码，查询value
     * @param paramCode 参数编码
     * @return          参数值
     */
    String getValueByCode(String paramCode);

    /**
     * 获取参数编码列表
     * @param ids  ids
     * @return     返回参数编码列表
     */
    List<String> getParamCodeList(Long[] ids);

    /**
     * 根据参数编码，更新value
     * @param paramCode  参数编码
     * @param paramValue  参数值
     */
    int updateValueByCode(@Param("paramCode") String paramCode, @Param("paramValue") String paramValue);
}
