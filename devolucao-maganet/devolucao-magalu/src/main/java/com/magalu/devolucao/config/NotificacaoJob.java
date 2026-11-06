package com.magalu.devolucao.config;
import com.magalu.devolucao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="app.notificacoes.job-ativo",havingValue="true",matchIfMissing=true)
public class NotificacaoJob {
    private final NotificacaoService service;
    private final NotificacaoProperties config;

    @Scheduled(fixedDelayString="${app.notificacoes.intervalo-ms:300000}", initialDelayString="${app.notificacoes.intervalo-ms:300000}")
    public void lembrar() {
        long inicio = System.nanoTime();
        int lotes = 0, inseridos = 0;
        for (int i = 0; i < config.getMaxLotesPorExecucao(); i++) {
            // Chamada via outro bean: cada lote abre e encerra sua transacao.
            var lote = service.gerarLembretes(config.getAntecedenciaMinutos(), config.getTamanhoLote());
            lotes++;
            inseridos += lote.processados();
            if (lote.selecionados() < config.getTamanhoLote()) break;
        }
        var pendentes = service.pendenciasLembretes(config.getAntecedenciaMinutos());
        log.info("job=lembretes lotes={} inseridos={} pendentes={} proximoVencimento={} duracaoMs={}",
                lotes, inseridos, pendentes.quantidade(), pendentes.proximoVencimento(),
                (System.nanoTime() - inicio) / 1000000);
        if (pendentes.quantidade() > 0)
            log.warn("Ainda ha {} lembretes pendentes; proximo vencimento={}. Verifique capacidade, limites e outros workers.",
                    pendentes.quantidade(), pendentes.proximoVencimento());
    }
}
