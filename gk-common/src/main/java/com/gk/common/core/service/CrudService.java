package com.gk.common.core.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;

import java.util.List;

/**
 *  CRUD基础服务接口
 *
 * @author Lowen
 */
public interface CrudService<T, D> extends BaseService<T> {

    PageData<D> page(DataMap params);

    List<D> list(DataMap params);

    D get(Long id);

    void save(D dto);

    void update(D dto);

    void delete(Long[] ids);

    void delete(Long id);

    List<T> list(Wrapper<T> queryWrapper);

}