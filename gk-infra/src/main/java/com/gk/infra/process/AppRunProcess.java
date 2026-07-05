package com.gk.infra.process;

import com.gk.common.tools.StringFormat;
import com.gk.common.utils.DateUtils;
import com.gk.infra.telegram.TgAlertService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.TimeZone;

@Slf4j
@Data
@Component
public class AppRunProcess implements ApplicationRunner {
    private final Environment environment;
    private final ObjectProvider<TgAlertService> tgAlertServiceProvider;

    @Value("${server.servlet.context-path: }")
    private String serverPath;

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${gk.lifecycle-alert.enabled:true}")
    private boolean lifecycleAlertEnabled;

    public AppRunProcess(Environment environment, ObjectProvider<TgAlertService> tgAlertServiceProvider) {
        this.environment = environment;
        this.tgAlertServiceProvider = tgAlertServiceProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        String ip = "localhost";
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        String activeProfile = activeProfile();
        String systemTimeZone = ZoneId.systemDefault().toString();
        String jvmTimeZone = TimeZone.getDefault().getID();

        log.warn("-----------------------------------------------------------------");
        log.warn("AppRunProcess started({}:http://{}:{}{}), env({}, TimeZone=UTC)",
                serverName, ip, serverPort, serverPath, activeProfile);
        log.warn("Linux system time zone: {}", systemTimeZone);
        log.warn("JVM default time zone: {}", jvmTimeZone);
        log.warn("-----------------------------------------------------------------");

        sendLifecycleAlert(activeProfile, systemTimeZone, jvmTimeZone);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("Uncaught exception in thread({})", thread.getName(), throwable));
    }

    private String activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return StringUtils.defaultIfBlank(environment.getProperty("spring.profiles.active"), "default");
        }
        return String.join(",", activeProfiles);
    }

    private void sendLifecycleAlert(String activeProfile, String systemTimeZone, String jvmTimeZone) {
        if (!lifecycleAlertEnabled) {
            return;
        }
        TgAlertService tgAlertService = tgAlertServiceProvider.getIfAvailable();
        if (tgAlertService == null) {
            log.debug("TgAlertService is not available, skip lifecycle alert: Service started");
            return;
        }
        String content = StringFormat.format("""
                APP: {}
                URL: http://localhost:{}{}
                Profile: {}
                System TimeZone: {}
                JVM TimeZone: {}
                Time: {}
                """, serverName, serverPort, StringUtils.trimToEmpty(serverPath),
                activeProfile, systemTimeZone, jvmTimeZone, DateUtils.now("GMT+08:00"));
        tgAlertService.sysWarn("✅ Service started(" + serverName + ")", content, "");
    }
}
