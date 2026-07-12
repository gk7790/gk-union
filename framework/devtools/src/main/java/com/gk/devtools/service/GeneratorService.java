package com.gk.devtools.service;


import com.gk.devtools.config.DataSourceInfo;
import com.gk.devtools.entity.MenuEntity;
import com.gk.devtools.entity.TableFieldEntity;
import com.gk.devtools.entity.TableInfoEntity;

import java.util.List;

/**
 * 代码生成
 *
 * @author Lowen
 */
public interface GeneratorService {

    DataSourceInfo getDataSourceInfo(Long datasourceId);

    void datasourceTable(TableInfoEntity tableInfo);

    void updateTableField(Long tableId, List<TableFieldEntity> tableFieldList);

    void generatorCode(TableInfoEntity tableInfo);

    void generatorMenu(MenuEntity menu);
}