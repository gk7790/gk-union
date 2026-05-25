package com.gk.infra.i18n.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.i18n.entity.SysI18nEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* 系统-国际化
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-23
*/
@Mapper
public interface SysI18nDao extends BaseDao<SysI18nEntity> {

    List<SysI18nEntity> selectByLang(String type, String lang);
}