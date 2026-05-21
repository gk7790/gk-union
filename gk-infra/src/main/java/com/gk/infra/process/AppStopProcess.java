package com.gk.infra.process;

import com.gk.common.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppStopProcess implements DisposableBean {

    @Override
    public void destroy() {
        String activeProfile = SpringContextUtils.getEnvValue("spring.profiles.active");
        log.warn("-----------------------------------------------------------------");
        log.warn("AppStopProcess: Resources are being prepared for release ....");
        log.warn("AppStopProcess: stop succeed({})", activeProfile);
        log.warn("-----------------------------------------------------------------");
    }
}
