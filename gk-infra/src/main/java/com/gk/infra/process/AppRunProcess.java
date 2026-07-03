package com.gk.infra.process;

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

        sendLifecycleAlert("Service started", activeProfile, systemTimeZone, jvmTimeZone);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread({})", thread.getName(), throwable);
        });
    }

    private String activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return StringUtils.defaultIfBlank(environment.getProperty("spring.profiles.active"), "default");
        }
        return String.join(",", activeProfiles);
    }

    private void sendLifecycleAlert(String title, String activeProfile, String systemTimeZone, String jvmTimeZone) {
        if (!lifecycleAlertEnabled) {
            return;
        }
        TgAlertService tgAlertService = tgAlertServiceProvider.getIfAvailable();
        if (tgAlertService == null) {
            log.debug("TgAlertService is not available, skip lifecycle alert: {}", title);
            return;
        }
        String content = "\n"
                + "Application: " + serverName + "\n"
                + "URL: http://localhost:" + serverPort + StringUtils.trimToEmpty(serverPath) + "\n"
                + "Profile: " + activeProfile + "\n"
                + "System TimeZone: " + systemTimeZone + "\n"
                + "JVM TimeZone: " + jvmTimeZone;
        tgAlertService.sysWarn(title + " - " + serverName, content, "");
    }
}
