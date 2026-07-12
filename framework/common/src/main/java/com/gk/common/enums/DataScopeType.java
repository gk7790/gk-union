package com.gk.common.enums;

/**
 * 数据权限范围类型。
 */
public enum DataScopeType {
    /**
     * 根据当前主体自动判断：平台不加过滤，租户加 tenant_id，商户加 tenant_id + merchant_id。
     */
    AUTO,
    /**
     * 租户级数据隔离。
     */
    TENANT,
    /**
     * 商户级数据隔离。
     */
    MERCHANT,
    /**
     * 部门级数据隔离。
     */
    DEPT,
    /**
     * 当前用户本人数据。
     */
    SELF,
    /**
     * 不做数据过滤，仅用于明确的平台全局查询。
     */
    NONE
}
