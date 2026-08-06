package com.wd.ms_enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.world_dance.wd_lib_common.security.JwtUtil;

@SpringBootApplication(
    scanBasePackages = {
        "com.wd.ms_enrollment",
        "com.world_dance.wd_lib_common"
    }
)
@EntityScan(basePackages = {
    "com.wd.ms_enrollment", 
    "com.world_dance.wd_lib_common.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.wd.ms_enrollment",
    "com.world_dance.wd_lib_common.repository"
})

@EnableDiscoveryClient
@EnableFeignClients
public class MsEnrollmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEnrollmentApplication.class, args);
    }

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}