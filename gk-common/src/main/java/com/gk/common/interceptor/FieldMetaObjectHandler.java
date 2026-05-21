package com.gk.common.interceptor;

import com.gk.common.context.ReqContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FieldMetaObjectHandler extends MysqlMetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = ReqContextHolder.getUserId();
        Long deptId = ReqContextHolder.getDeptId();
        Long tenantId = ReqContextHolder.getTenantId();

        Instant now = Instant.now();

        //创建者所属部门
        strictInsertFill(metaObject, DEPT_ID, Long.class, deptId);

        //创建者所属部门
        strictInsertFill(metaObject, TENANT_ID, Long.class, tenantId);

        //创建者
        strictInsertFill(metaObject, CREATED_BY, Long.class, userId);
        //创建时间
        strictInsertFill(metaObject, CREATED_AT, Instant.class, now);

        //更新者
        strictInsertFill(metaObject, UPDATED_BY, Long.class, userId);
        //更新时间
        strictInsertFill(metaObject, UPDATED_AT, Instant.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //更新者
        strictUpdateFill(metaObject, UPDATED_BY, Long.class, ReqContextHolder.getUserId());
        //更新时间
        strictUpdateFill(metaObject, UPDATED_AT, Instant.class, Instant.now());
    }
}
