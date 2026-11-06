package com.magalu.devolucao.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.*;

class ConfiguracaoJobsTest {
    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties({NotificacaoProperties.class,ExpiracaoProperties.class})
    static class Config {}
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test void padroesSaoValidos() {runner.run(c->assertThat(c).hasNotFailed());}
    @Test void janelaMenorQueIntervaloImpedeInicializacao() {
        runner.withPropertyValues("app.notificacoes.antecedencia-minutos=1","app.notificacoes.intervalo-ms=300000")
                .run(c->assertThat(c).hasFailed());
    }
    @Test void janelaIgualAoIntervaloTambemEhRejeitada() {
        runner.withPropertyValues("app.notificacoes.antecedencia-minutos=5","app.notificacoes.intervalo-ms=300000")
                .run(c->assertThat(c).hasFailed());
    }
    @Test void janelaCurtaFuncionaComIntervaloMenor() {
        runner.withPropertyValues("app.notificacoes.antecedencia-minutos=1","app.notificacoes.intervalo-ms=30000")
                .run(c->assertThat(c).hasNotFailed());
    }
    @Test void loteInvalidoEhRejeitado() {
        runner.withPropertyValues("app.notificacoes.tamanho-lote=0").run(c->assertThat(c).hasFailed());
        runner.withPropertyValues("app.qrcode.max-lotes-expiracao=0").run(c->assertThat(c).hasFailed());
    }
}
