package com.wd.ms_enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.world_dance.wd_lib_common.security.JwtUtil;

@SpringBootApplication(
    // 1. Corregido: Escanea los componentes usando el prefijo correcto de tu librería
    scanBasePackages = {
        "com.wd.ms_enrollment",
        "com.world_dance.wd_lib_common"
    }
)
// 2. Corregido: Apunta al paquete real de las entidades de la librería
@EntityScan(basePackages = {
    "com.wd.ms_enrollment", 
    "com.world_dance.wd_lib_common.entity"
})
// 3. Corregido: Apunta al paquete real de los repositorios comunes si los usas
@EnableJpaRepositories(basePackages = {
    "com.wd.ms_enrollment",
    "com.world_dance.wd_lib_common.repository"
})
public class MsEnrollmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEnrollmentApplication.class, args);
    }

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}