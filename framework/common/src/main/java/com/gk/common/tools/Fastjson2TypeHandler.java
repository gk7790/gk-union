package com.gk.common.tools;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Fastjson2TypeHandler<T> extends BaseTypeHandler<T> {

    private final Class<T> clazz;

    public Fastjson2TypeHandler(Class<T> type) {
        clazz = type;
    }

    private T parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return JSON.parseObject(value, clazz);
    }

    protected String toJson(T obj) {
        return JSON.toJSONString(obj);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

}