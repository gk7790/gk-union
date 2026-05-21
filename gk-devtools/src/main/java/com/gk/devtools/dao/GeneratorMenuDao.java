package com.gk.devtools.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 创建菜单
 *
 * @author Lowen
 */
@Mapper
public interface GeneratorMenuDao {

    void generatorMenu(Map<String, Object> params);

    void generatorMenuLanguage(Map<String, Object> params);
}