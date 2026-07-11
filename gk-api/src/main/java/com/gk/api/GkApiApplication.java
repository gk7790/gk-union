package com.gk.api;

import com.gk.api.config.ApiComponentExcludeFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;

import java.util.TimeZone;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.gk.**.dao")
@ComponentScan(
        basePackages = "com.gk",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = ApiComponentExcludeFilter.class)
        }
)
public class GkApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(GkApiApplication.class, args);
    }
}
