package com.magalu.devolucao.service;
import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.dto.PreferenciasNotificacao;
import com.magalu.devolucao.repository.NotificacaoRepository;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class NotificacaoServiceTest {
    final NotificacaoRepository repo=mock(NotificacaoRepository.class);
    final IdentidadeService identidade=mock(IdentidadeService.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"),ZoneId.of("America/Sao_Paulo"));
    final NotificacaoService service=new NotificacaoService(repo,identidade,clock);

    @Test void usaMensagemPublicaSemDadosInternos() {
        var s=SolicitacaoDevolucao.builder().id(2L).status(com.magalu.devolucao.domain.enums.StatusSolicitacao.DESTINO_PENDENTE).usuario(Usuario.builder().id(3L).build()).build();
        var evento=RegistroAuditoria.builder().id(9L).solicitacao(s).acao("DESTINO_DEFINIDO")
                .autor("SEGREDO_AUTOR").detalhes("SEGREDO_DETALHES").timestamp(LocalDateTime.now(clock)).build();
        service.registrarEvento(evento);
        verify(repo).inserir(eq(3L),eq(2L),eq("evento:9"),eq("DESTINO_DEFINIDO"),
                argThat(m->m.contains("aguarda processamento") && !m.contains("SEGREDO")),
                eq(LocalDateTime.now(clock)),isNull(),eq(false));
    }
    @Test void ignoraEventoInternoOuConclusaoNaoImplementada() {
        service.registrarEvento(RegistroAuditoria.builder().acao("INTERNO").build());
        service.registrarEvento(RegistroAuditoria.builder().acao("CONCLUIDA").build());
        verifyNoInteractions(repo);
    }
    @Test void revalidaAntesDeLembrarCodigoQueFoiCancelado() {
        var agora=LocalDateTime.now(clock);
        var lembrete=new NotificacaoRepository.Lembrete(4,2,3,agora.plusMinutes(20));
        when(repo.bloquearLembretes(agora,agora.plusMinutes(120),200)).thenReturn(List.of(lembrete));
        when(repo.lembreteAindaValido(lembrete,agora)).thenReturn(false);
        assertThat(service.gerarLembretes(120,200).processados()).isZero();
        verify(repo,never()).inserir(anyLong(),anyLong(),anyString(),anyString(),anyString(),any(),any(),anyBoolean());
    }
    @Test void lembreteUsaClockEPrazoDoQr() {
        var agora=LocalDateTime.now(clock);
        var lembrete=new NotificacaoRepository.Lembrete(4,2,3,agora.plusMinutes(20));
        when(repo.bloquearLembretes(agora,agora.plusMinutes(120),200)).thenReturn(List.of(lembrete));
        when(repo.lembreteAindaValido(lembrete,agora)).thenReturn(true);
        when(repo.inserir(anyLong(),anyLong(),anyString(),anyString(),anyString(),any(),any(),eq(true))).thenReturn(1);
        assertThat(service.gerarLembretes(120,200).processados()).isEqualTo(1);
        verify(repo).inserir(eq(3L),eq(2L),eq("qr:4"),eq("QR_PRESTES_A_EXPIRAR"),anyString(),eq(agora),eq(lembrete.expiraEm()),eq(true));
    }
    @Test void leituraDeOutroClienteNaoEhAceita() {
        when(identidade.cliente()).thenReturn(Usuario.builder().id(3L).build());
        when(repo.marcarLida(3L,9L,LocalDateTime.now(clock))).thenReturn(0);
        assertThatThrownBy(()->service.marcarLida(9L)).hasMessage("Notificacao nao encontrada");
    }
    @Test void rejeitaParametrosInvalidos() {
        assertThatThrownBy(()->service.gerarLembretes(0,200)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.gerarLembretes(1441,200)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.listar(false,0,101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.salvarPreferencias(new PreferenciasNotificacao(null,true))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }
    @Test void conflitoOuPreferenciaDesativadaNaoContaComoEnviado() {
        var agora=LocalDateTime.now(clock);
        var lembrete=new NotificacaoRepository.Lembrete(4,2,3,agora.plusMinutes(20));
        when(repo.bloquearLembretes(agora,agora.plusMinutes(120),200)).thenReturn(List.of(lembrete));
        when(repo.lembreteAindaValido(lembrete,agora)).thenReturn(true);
        // INSERT retorna zero se o evento ja existe ou a preferencia foi desativada.
        assertThat(service.gerarLembretes(120,200)).isEqualTo(new ResultadoLote(1,0));
    }
    @Test void historicoDeDestinoNaoAfirmaQueAindaEstaPendente() {
        assertThat(com.magalu.devolucao.domain.enums.EventoCliente.DESTINO_DEFINIDO.mensagem())
                .isEqualTo("O destino do produto foi definido.");
    }
}
