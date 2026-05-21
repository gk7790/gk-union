package com.gk.common.interceptor;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

public abstract class MysqlMetaObjectHandler implements MetaObjectHandler {
    public static final String CREATED_BY = "createdBy";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_BY = "updatedBy";
    public static final String UPDATED_AT = "updatedAt";
    public static final String DEPT_ID = "deptId";
    public static final String TENANT_ID = "tenantId";
}
