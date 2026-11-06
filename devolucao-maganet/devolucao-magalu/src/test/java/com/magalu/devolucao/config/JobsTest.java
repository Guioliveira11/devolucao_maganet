package com.magalu.devolucao.config;

import com.magalu.devolucao.repository.NotificacaoRepository;
import com.magalu.devolucao.service.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class JobsTest {
    @Test void drenaMaisDeDuzentosLembretesNaMesmaExecucao(CapturedOutput output) {
        var service = mock(NotificacaoService.class);
        var config = new NotificacaoProperties();
        when(service.gerarLembretes(120,200)).thenReturn(new ResultadoLote(200,200),
                new ResultadoLote(200,198),new ResultadoLote(1,1));
        when(service.pendenciasLembretes(120)).thenReturn(new NotificacaoRepository.Pendencias(0,null));
        new NotificacaoJob(service,config).lembrar();
        verify(service,times(3)).gerarLembretes(120,200);
        assertThat(output.getOut()).contains("inseridos=399 pendentes=0");
    }

    @Test void limitaExecucaoEAvisaQuandoHaPendencias(CapturedOutput output) {
        var service = mock(NotificacaoService.class);
        var config = new NotificacaoProperties();config.setMaxLotesPorExecucao(2);
        when(service.gerarLembretes(120,200)).thenReturn(new ResultadoLote(200,200));
        when(service.pendenciasLembretes(120)).thenReturn(new NotificacaoRepository.Pendencias(17,
                LocalDateTime.of(2026,9,26,12,0)));
        new NotificacaoJob(service,config).lembrar();
        verify(service,times(2)).gerarLembretes(120,200);
        assertThat(output.getOut()).contains("pendentes=17", "Ainda ha 17 lembretes pendentes");
    }

    @Test void expiraVariosLotesESaiAoEsgotarFila() {
        var service = mock(QRCodeService.class);
        when(service.expirarCodigosVencidos(100)).thenReturn(new ResultadoLote(100,100),new ResultadoLote(2,2));
        new QRCodeExpirationJob(service,new ExpiracaoProperties()).expirarCodigosVencidos();
        verify(service,times(2)).expirarCodigosVencidos(100);
        verify(service).contarExpiracoesPendentes();
    }

    @Test void limiteDeExpiracaoEvitaLoopSemFim(CapturedOutput output) {
        var service = mock(QRCodeService.class);
        var config = new ExpiracaoProperties();config.setMaxLotesExpiracao(2);
        when(service.expirarCodigosVencidos(100)).thenReturn(new ResultadoLote(100,100));
        when(service.contarExpiracoesPendentes()).thenReturn(3L);
        new QRCodeExpirationJob(service,config).expirarCodigosVencidos();
        verify(service,times(2)).expirarCodigosVencidos(100);
        assertThat(output.getOut()).contains("Ainda ha 3 expiracoes pendentes");
    }
}
