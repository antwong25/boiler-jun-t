package org.example.boilerserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("org.example.boilerserver.config")
@MapperScan("org.example.boilerserver.mapper")
public class BoilerServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoilerServerApplication.class, args);
    }

}
