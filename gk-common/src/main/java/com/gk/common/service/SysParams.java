package com.gk.common.service;

public interface SysParams {

    /**
     * 根据参数编码，获取参数的value值
     *
     * @param paramCode  参数编码
     */
    String getValue(String paramCode);

    /**
     * 根据参数编码，获取value的Object对象
     * @param paramCode  参数编码
     * @param clazz  Object对象
     */
    <T> T getValueObject(String paramCode, Class<T> clazz);

}
