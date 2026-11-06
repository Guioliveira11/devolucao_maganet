package com.magalu.devolucao.config;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component @ConfigurationProperties(prefix="app.qrcode") @Validated @Getter @Setter
public class ExpiracaoProperties {
    @Min(1000) private long jobExpiracaoMs = 300000;
    @Min(1) @Max(1000) private int tamanhoLoteExpiracao = 100;
    @Min(1) @Max(1000) private int maxLotesExpiracao = 50;
}
