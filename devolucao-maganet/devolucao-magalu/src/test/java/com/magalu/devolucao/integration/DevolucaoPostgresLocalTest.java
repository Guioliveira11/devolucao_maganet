package com.magalu.devolucao.integration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
/** Somente banco descartavel de testes: a suite trunca os dados entre testes. */
@EnabledIfEnvironmentVariable(named="DEVOLUCAO_TEST_DB_URL",matches=".+")
class DevolucaoPostgresLocalTest extends AbstractDevolucaoPostgresTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("DEVOLUCAO_TEST_DB_URL"));
        r.add("spring.datasource.username",()->System.getenv("DEVOLUCAO_TEST_DB_USER"));
        r.add("spring.datasource.password",()->System.getenv().getOrDefault("DEVOLUCAO_TEST_DB_PASSWORD",""));
    }
}
