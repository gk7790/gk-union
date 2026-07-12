package com.gk.infra.log.service;

import com.gk.infra.log.dao.LogErrorDao;
import com.gk.infra.log.entity.LogErrorEntity;
import com.gk.infra.utils.AsynUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统异常日志
 * @author Lowen lowen@gmail.com
 * @since 3.0 2026-05-29
 */
@Service
@RequiredArgsConstructor
public class LogErrorService {
    private final LogErrorDao logErrorDao;

    public void asyncAdd(LogErrorEntity log) {
        AsynUtils.execute("异步添加异常信息", ()->{
            logErrorDao.insert(log);
        });
    }
}