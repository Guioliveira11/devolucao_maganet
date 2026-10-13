package com.magalu.devolucao.config;
import com.magalu.devolucao.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class QRCodeExpirationJob {
    private final QRCodeService service;
    private final ExpiracaoProperties config;

    @Scheduled(fixedDelayString="${app.qrcode.job-expiracao-ms:300000}")
    public void expirarCodigosVencidos() {
        long inicio = System.nanoTime();
        int lotes = 0, expirados = 0;
        for (int i = 0; i < config.getMaxLotesExpiracao(); i++) {
            var lote = service.expirarCodigosVencidos(config.getTamanhoLoteExpiracao());
            lotes++;
            expirados += lote.processados();
            if (lote.selecionados() < config.getTamanhoLoteExpiracao()) break;
        }
        long pendentes = service.contarExpiracoesPendentes();
        log.info("job=expiracao lotes={} expirados={} pendentes={} duracaoMs={}",
                lotes, expirados, pendentes, (System.nanoTime() - inicio) / 1000000);
        if (pendentes > 0) log.warn("Ainda ha {} expiracoes pendentes; verifique capacidade, limites e outros workers.", pendentes);
    }
}
