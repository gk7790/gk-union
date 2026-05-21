package com.gk.infra.process;

import com.gk.common.utils.SpringContextUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.TimeZone;


@Slf4j
@Data
@Component
public class AppRunProcess implements ApplicationRunner {

    @Value("${server.servlet.context-path: }")
    private String serverPath;

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private int serverPort;

    @Override
    public void run(ApplicationArguments args) {
        String ip = "localhost";
        // 默认使用 UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        String activeProfile = SpringContextUtils.getEnvValue("spring.profiles.active");
        log.warn("-----------------------------------------------------------------");
        log.warn("AppRunProcess started({}:http://{}:{}{}), 环境({}, TimeZone=UTC)", serverName, ip, serverPort, serverPath, activeProfile);
        log.warn("✅ Linux 系统时区: {}", ZoneId.systemDefault());
        log.warn("✅ JVM 默认时区: {}", TimeZone.getDefault().getID());
        log.warn("-----------------------------------------------------------------");

        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("线程({})中异常:", thread.getName(), throwable);
        });
    }
}
