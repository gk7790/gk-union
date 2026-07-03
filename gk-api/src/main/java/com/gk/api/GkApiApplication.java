package com.gk.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.gk",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
                        "com\\.gk\\.auth\\.config\\..*",
                        "com\\.gk\\.payment\\.controller\\..*",
                        "com\\.gk\\.merchant\\.controller\\..*",
                        "com\\.gk\\.psp\\.controller\\..*",
                        "com\\.gk\\.ledger\\.controller\\..*",
                        "com\\.gk\\.adjustment\\.controller\\..*",
                        "com\\.gk\\.dashboard\\.controller\\..*",
                        "com\\.gk\\.tenant\\.controller\\..*",
                        "com\\.gk\\.iam\\.controller\\..*",
                        "com\\.gk\\.meta\\.controller\\..*",
                        "com\\.gk\\.infra\\.controller\\..*",
                        "com\\.gk\\.infra\\..*\\.controller\\..*",
                        "com\\.gk\\.quartz\\..*",
                        "com\\.gk\\.telegram\\..*",
                        "com\\.gk\\.devtools\\..*"
                })
        }
)
public class GkApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(GkApiApplication.class, args);
    }
}
