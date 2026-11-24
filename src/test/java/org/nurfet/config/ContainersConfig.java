package org.nurfet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@Slf4j
public class ContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer("postgres:18");
    }
}
