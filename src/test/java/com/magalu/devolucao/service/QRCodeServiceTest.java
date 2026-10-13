package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.exception.QRCodeInvalidoException;
import com.magalu.devolucao.repository.SolicitacaoDevolucaoRepository;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QRCodeServiceTest {
    final java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-19T15:00:00Z"), java.time.ZoneId.of("America/Sao_Paulo"));
    SolicitacaoDevolucaoRepository repo;
    QRCodeService service;
    SolicitacaoDevolucao solicitacao;
    @BeforeEach void preparar() {
        repo=mock(SolicitacaoDevolucaoRepository.class); service=new QRCodeService(clock, mock(com.magalu.devolucao.repository.RegistroAuditoriaRepository.class), mock(NotificacaoService.class), repo);
        ReflectionTestUtils.setField(service,"validadeHoras",24);
        ReflectionTestUtils.setField(service,"tamanhoPx",300);
        solicitacao=SolicitacaoDevolucao.builder().id(1L).status(StatusSolicitacao.QR_EXPIRADO).build();
    }
    QRCodeDevolucao adicionar(boolean vencido) {
        var qr=QRCodeDevolucao.builder().token(UUID.randomUUID()).status(StatusQRCode.ATIVO)
                .dataExpiracao(LocalDateTime.now(clock).plusHours(vencido ? -1 : 1)).build();
        solicitacao.adicionarQrCode(qr);return qr;
    }
    @Test void renovaComImagemENovoToken() {
        var antigo=adicionar(true); var novo=service.gerarQRCode(solicitacao);
        assertThat(novo.getToken()).isNotEqualTo(antigo.getToken());
        assertThat(novo.getImagemBase64()).isNotBlank(); assertThat(novo.isValido(LocalDateTime.now(clock))).isTrue();
        assertThat(antigo.getStatus()).isEqualTo(StatusQRCode.EXPIRADO);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
    }
    @Test void jobNaoExpiraNovoQrPorCausaDeHistoricoAntigo() {
        adicionar(true); var novo=adicionar(false); solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        when(repo.findVencidasParaAtualizacao(any(),eq(100))).thenReturn(List.of(solicitacao));
        service.expirarCodigosVencidos(100);
        assertThat(novo.isValido(LocalDateTime.now(clock))).isTrue();
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
    }
    @Test void jobExpiraVencido() {
        var qr=adicionar(true); solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        when(repo.findVencidasParaAtualizacao(any(),eq(100))).thenReturn(List.of(solicitacao));
        service.expirarCodigosVencidos(100);
        assertThat(qr.getStatus()).isEqualTo(StatusQRCode.EXPIRADO);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.QR_EXPIRADO);
    }
    @Test void codigoAntigoNaoValeDepoisDeRenovacao() {
        var antigo=adicionar(true); service.gerarQRCode(solicitacao);
        when(repo.findIdByQrToken(antigo.getToken())).thenReturn(Optional.of(1L));
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.validarQRCode(antigo.getToken())).isInstanceOf(QRCodeInvalidoException.class);
    }
    @Test void codigoCanceladoNaoPodeSerUtilizado() {
        var qr=adicionar(false); qr.cancelar(); solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        when(repo.findIdByQrToken(qr.getToken())).thenReturn(Optional.of(1L));
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.validarQRCode(qr.getToken())).isInstanceOf(QRCodeInvalidoException.class);
    }
    @Test void falhaTecnicaNaImagemNaoEhConflitoDeNegocio() {
        ReflectionTestUtils.setField(service,"tamanhoPx",-1);
        assertThatThrownBy(()->service.gerarQRCode(solicitacao))
                .isInstanceOf(com.magalu.devolucao.exception.FalhaGeracaoQRCodeException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
    @Test void expiracaoRespeitaLimiteEContaAlteracoes() {
        adicionar(true);solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        when(repo.findVencidasParaAtualizacao(any(),eq(1))).thenReturn(List.of(solicitacao));
        assertThat(service.expirarCodigosVencidos(1)).isEqualTo(new ResultadoLote(1,1));
        verify(repo).findVencidasParaAtualizacao(LocalDateTime.now(clock),1);
    }
}
