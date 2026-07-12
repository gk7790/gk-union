package com.gk.infra.process;

import com.gk.common.tools.StringFormat;
import com.gk.common.utils.DateUtils;
import com.gk.infra.notify.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppStopProcess implements DisposableBean {
    private final Environment environment;
    private final ObjectProvider<NotifyService> notifyServiceProvider;

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${server.servlet.context-path: }")
    private String serverPath;

    @Value("${gk.lifecycle-alert.enabled:true}")
    private boolean lifecycleAlertEnabled;

    public AppStopProcess(Environment environment, ObjectProvider<NotifyService> notifyServiceProvider) {
        this.environment = environment;
        this.notifyServiceProvider = notifyServiceProvider;
    }

    @Override
    public void destroy() {
        String activeProfile = activeProfile();
        log.warn("-----------------------------------------------------------------");
        log.warn("AppStopProcess: Resources are being prepared for release ....");
        log.warn("AppStopProcess: stop succeed({})", activeProfile);
        log.warn("-----------------------------------------------------------------");
        sendLifecycleAlert(activeProfile);
    }

    private String activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return StringUtils.defaultIfBlank(environment.getProperty("spring.profiles.active"), "default");
        }
        return String.join(",", activeProfiles);
    }

    private void sendLifecycleAlert(String activeProfile) {
        if (!lifecycleAlertEnabled) {
            return;
        }
        NotifyService notifyService = notifyServiceProvider.getIfAvailable();
        if (notifyService == null) {
            log.debug("NotifyService is not available, skip lifecycle alert: Service stopped");
            return;
        }
        String content = StringFormat.format("""
                ⛔ Service stopped - {}
                ──────────────
                APP: {}
                URL: http://localhost:{}{}
                Profile: {}
                Time: {}
                """, serverName, serverName, serverPort, StringUtils.trimToEmpty(serverPath),
                activeProfile, DateUtils.now("GMT+08:00"));
        notifyService.sysWarnSync(content, "");
    }
}
