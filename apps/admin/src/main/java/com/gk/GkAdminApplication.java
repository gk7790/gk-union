package com.gk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

import java.util.TimeZone;

@SpringBootApplication()
@MapperScan("com.gk.**.dao")
public class GkAdminApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(GkAdminApplication.class, args);
    }

}
