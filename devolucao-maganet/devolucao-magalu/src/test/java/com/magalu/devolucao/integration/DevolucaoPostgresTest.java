package com.magalu.devolucao.integration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
@Testcontainers(disabledWithoutDocker=true)
@DisabledIfEnvironmentVariable(named="DEVOLUCAO_TEST_DB_URL",matches=".+")
class DevolucaoPostgresTest extends AbstractDevolucaoPostgresTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);
        r.add("spring.datasource.username",postgres::getUsername);
        r.add("spring.datasource.password",postgres::getPassword);
    }
}
