package com.gk.common.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页工具类
 *
 * @author Lowen
 */
@Data
public class PageData<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int total;

    private List<T> items;

    /**
     * 分页
     * @param list   列表数据
     * @param total  总记录数
     */
    public PageData(List<T> list, long total) {
        this.items = list;
        this.total = (int)total;
    }
}
