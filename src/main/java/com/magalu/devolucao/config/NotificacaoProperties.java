package com.magalu.devolucao.config;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component @ConfigurationProperties(prefix="app.notificacoes") @Validated @Getter @Setter
public class NotificacaoProperties {
    @Min(1) @Max(1440) private int antecedenciaMinutos = 120;
    @Min(1000) private long intervaloMs = 300000;
    @Min(1) @Max(1000) private int tamanhoLote = 200;
    @Min(1) @Max(1000) private int maxLotesPorExecucao = 50;
    @AssertTrue(message="antecedencia-minutos deve ser maior que intervalo-ms convertido em minutos")
    public boolean isJanelaValida() { return antecedenciaMinutos * 60000L > intervaloMs; }
}
