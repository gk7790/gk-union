package com.gk.devtools.service.impl;

import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.devtools.dao.TableFieldDao;
import com.gk.devtools.entity.TableFieldEntity;
import com.gk.devtools.service.TableFieldService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 表
 *
 * @author Lowen
 */
@Service
public class TableFieldServiceImpl extends BaseServiceImpl<TableFieldDao, TableFieldEntity> implements TableFieldService {

    @Override
    public List<TableFieldEntity> getByTableName(String tableName) {
        return baseDao.getByTableName(tableName);
    }

    @Override
    public void deleteByTableName(String tableName) {
        baseDao.deleteByTableName(tableName);
    }

    @Override
    public void deleteBatchTableIds(Long[] tableIds) {
        baseDao.deleteBatchTableIds(tableIds);
    }

}