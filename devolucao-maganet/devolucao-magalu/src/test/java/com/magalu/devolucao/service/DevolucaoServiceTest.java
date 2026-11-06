package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.exception.*;
import com.magalu.devolucao.repository.*;
import com.magalu.devolucao.service.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucaoServiceTest {
    final java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-19T15:00:00Z"), java.time.ZoneId.of("America/Sao_Paulo"));
    @Mock SolicitacaoDevolucaoRepository solicitacoes;
    @Mock ItemCompradoRepository itens;
    @Mock LojaRepository lojas;
    @Mock RegistroAuditoriaRepository auditoria;
    @Mock IdentidadeService identidade;
    @Mock QRCodeService qr;
    @Mock DestinoStrategyResolver destinos;
    DevolucaoService service;
    Usuario cliente;
    Loja loja;
    ItemComprado item;
    SolicitacaoDevolucao solicitacao;

    @BeforeEach void preparar() {
        service = new DevolucaoService(clock, mock(NotificacaoService.class), solicitacoes, itens, lojas, auditoria, identidade, qr, destinos);
        cliente = Usuario.builder().id(1L).build();
        loja = Loja.builder().id(10L).ativa(true).build();
        var pedido = Pedido.builder().numeroPedido("P1").usuario(cliente).build();
        item = ItemComprado.builder().id(20L).pedido(pedido).quantidade(1)
                .produto(Produto.builder().nome("Produto").build())
                .dataLimiteDevolucao(LocalDate.now(clock).plusDays(2)).build();
        solicitacao = SolicitacaoDevolucao.builder().id(30L).usuario(cliente)
                .itemComprado(item).lojaEscolhida(loja)
                .status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO).build();
    }
    void criacao() {
        when(identidade.cliente()).thenReturn(cliente);
        when(itens.findByIdForUpdate(20L)).thenReturn(Optional.of(item));
    }
    void acaoCliente() {
        when(identidade.cliente()).thenReturn(cliente);
        when(solicitacoes.findByIdForUpdate(30L)).thenReturn(Optional.of(solicitacao));
    }
    QRCodeDevolucao codigo(boolean vencido) {
        var codigo = QRCodeDevolucao.builder().token(UUID.randomUUID()).status(StatusQRCode.ATIVO)
                .dataExpiracao(LocalDateTime.now(clock).plusHours(vencido ? -1 : 1)).build();
        solicitacao.adicionarQrCode(codigo);
        return codigo;
    }
    @Test void rejeitaItemDeOutroCliente() {
        criacao(); item.getPedido().setUsuario(Usuario.builder().id(2L).build());
        assertThatThrownBy(() -> service.criarSolicitacao(20L,10L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(qr);
    }
    @Test void rejeitaLojaInativa() {
        criacao(); loja.setAtiva(false); when(lojas.findById(10L)).thenReturn(Optional.of(loja));
        assertThatThrownBy(() -> service.criarSolicitacao(20L,10L)).hasMessage("Loja inativa");
        verify(solicitacoes, never()).save(any());
    }
    @Test void rejeitaDuplicada() {
        criacao(); when(solicitacoes.existsByItemCompradoIdAndStatusNot(20L,StatusSolicitacao.CANCELADA)).thenReturn(true);
        assertThatThrownBy(() -> service.criarSolicitacao(20L,10L)).hasMessageContaining("aberta ou concluida");
        verifyNoInteractions(qr);
    }
    @Test void rejeitaPrazoVencido() {
        criacao(); item.setDataLimiteDevolucao(LocalDate.now(clock).minusDays(1));
        assertThatThrownBy(() -> service.criarSolicitacao(20L,10L)).hasMessageContaining("fora do prazo");
    }
    @Test void criaQuandoNaoHaDevolucaoNaoCancelada() {
        criacao(); when(lojas.findById(10L)).thenReturn(Optional.of(loja));
        assertThat(service.criarSolicitacao(20L,10L).status()).isEqualTo(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        verify(qr).gerarQRCode(any()); verify(solicitacoes).saveAndFlush(any());
    }
    @Test void cancelaEInvalidaQr() {
        acaoCliente(); var codigo = codigo(false);
        assertThat(service.cancelar(30L).status()).isEqualTo(StatusSolicitacao.CANCELADA);
        assertThat(codigo.getStatus()).isEqualTo(StatusQRCode.CANCELADO);
        assertThat(codigo.isValido(LocalDateTime.now(clock))).isFalse();
    }
    @Test void cancelarNovamenteNaoDuplicaAuditoria() {
        acaoCliente(); solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        service.cancelar(30L); verifyNoInteractions(auditoria);
    }
    @Test void naoCancelaAposRecebimento() {
        acaoCliente(); solicitacao.setStatus(StatusSolicitacao.EM_ANALISE);
        assertThatThrownBy(() -> service.cancelar(30L)).isInstanceOf(TransicaoInvalidaException.class);
    }
    @Test void naoCancelaDeOutroCliente() {
        acaoCliente(); solicitacao.setUsuario(Usuario.builder().id(2L).build());
        assertThatThrownBy(() -> service.cancelar(30L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void renovaQrExpirado() {
        acaoCliente(); codigo(true); solicitacao.setStatus(StatusSolicitacao.QR_EXPIRADO);
        service.renovarQRCode(30L); verify(qr).gerarQRCode(solicitacao);
    }
    @Test void renovaVencidoAntesDoJob() {
        acaoCliente(); codigo(true); service.renovarQRCode(30L); verify(qr).gerarQRCode(solicitacao);
    }
    @Test void naoTrocaQrAindaValido() {
        acaoCliente(); var codigo = codigo(false); service.renovarQRCode(30L);
        verifyNoInteractions(qr); assertThat(solicitacao.qrCodeAtivo(LocalDateTime.now(clock))).isSameAs(codigo);
    }
    @Test void naoRenovaDeOutroCliente() {
        acaoCliente(); solicitacao.setUsuario(Usuario.builder().id(2L).build());
        assertThatThrownBy(() -> service.renovarQRCode(30L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void naoRenovaCancelada() {
        acaoCliente(); solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        assertThatThrownBy(() -> service.renovarQRCode(30L)).isInstanceOf(TransicaoInvalidaException.class);
    }
    @Test void naoRenovaForaDoPrazo() {
        acaoCliente(); item.setDataLimiteDevolucao(LocalDate.now(clock).minusDays(1));
        assertThatThrownBy(() -> service.renovarQRCode(30L)).hasMessageContaining("fora do prazo");
    }
    @Test void naoRenovaEmLojaInativa() {
        acaoCliente(); loja.setAtiva(false);
        assertThatThrownBy(() -> service.renovarQRCode(30L)).hasMessage("Loja inativa");
    }
    @Test void listaSomentePaginaDoClienteAutenticado() {
        when(identidade.cliente()).thenReturn(cliente);
        var pageable = org.springframework.data.domain.PageRequest.of(2, 20, org.springframework.data.domain.Sort.by("id").descending());
        when(itens.findByPedido_Usuario_Id(1L,pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item),pageable,41));
        var pagina = service.listarItens(2,20);
        assertThat(pagina.total()).isEqualTo(41);
        assertThat(pagina.pagina()).isEqualTo(2);
        assertThat(pagina.itens()).singleElement().satisfies(i -> {
            assertThat(i.id()).isEqualTo(20L); assertThat(i.elegivelParaDevolucao()).isTrue();
        });
        verify(solicitacoes).findItensBloqueados(1L,List.of(20L));
    }
    @Test void variosItensConsultamSomenteBloqueiosDaPagina() {
        when(identidade.cliente()).thenReturn(cliente);
        var outro = ItemComprado.builder().id(21L).pedido(item.getPedido()).produto(item.getProduto())
                .dataLimiteDevolucao(LocalDate.now(clock)).build();
        when(itens.findByPedido_Usuario_Id(eq(1L),any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item,outro)));
        when(solicitacoes.findItensBloqueados(1L,List.of(20L,21L))).thenReturn(Set.of(20L));
        assertThat(service.listarItens(0,20).itens()).extracting(com.magalu.devolucao.dto.ItemCompradoResponse::elegivelParaDevolucao)
                .containsExactly(false, true);
        verify(solicitacoes).findItensBloqueados(1L,List.of(20L,21L));
        verify(solicitacoes, never()).existsByItemCompradoIdAndStatusNot(anyLong(), any());
    }
    @Test void paginaVaziaNaoConsultaBloqueios() {
        when(identidade.cliente()).thenReturn(cliente);
        when(itens.findByPedido_Usuario_Id(eq(1L),any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertThat(service.listarItens(0,20).itens()).isEmpty();
        verifyNoInteractions(solicitacoes);
    }
    @Test void rejeitaPaginacaoDeItensInvalidaAntesDeConsultarBanco() {
        for (var par : new int[][]{{-1,20},{0,0},{0,101}})
            assertThatThrownBy(() -> service.listarItens(par[0],par[1])).isInstanceOf(com.magalu.devolucao.exception.ParametroInvalidoException.class);
        verifyNoInteractions(itens,solicitacoes);
    }
    @Test void consultaDeOutroClienteNegada() {
        when(identidade.cliente()).thenReturn(cliente);
        when(solicitacoes.findById(30L)).thenReturn(Optional.of(solicitacao));
        solicitacao.setUsuario(Usuario.builder().id(2L).build());
        assertThatThrownBy(() -> service.buscarPorId(30L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void funcionarioDeOutraLojaNaoEscaneia() {
        var funcionario = Funcionario.builder().loja(Loja.builder().id(99L).build()).build();
        when(identidade.funcionario()).thenReturn(funcionario);
        var token = UUID.randomUUID(); when(qr.validarQRCode(token)).thenReturn(solicitacao);
        assertThatThrownBy(() -> service.processarEscaneamento(token)).isInstanceOf(AccessDeniedException.class);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
    }
    @Test void funcionarioDaLojaEscaneia() {
        var funcionario = Funcionario.builder().loja(loja).matricula("F1").build();
        when(identidade.funcionario()).thenReturn(funcionario);
        var codigo = codigo(false); when(qr.validarQRCode(codigo.getToken())).thenReturn(solicitacao);
        assertThat(service.processarEscaneamento(codigo.getToken()).status()).isEqualTo(StatusSolicitacao.EM_ANALISE);
        assertThat(codigo.getStatus()).isEqualTo(StatusQRCode.UTILIZADO);
        assertThat(solicitacao.getFuncionarioResponsavel()).isSameAs(funcionario);
    }
    @Test void funcionarioDeOutraLojaNaoDefineDestino() {
        when(identidade.funcionario()).thenReturn(Funcionario.builder().loja(Loja.builder().id(99L).build()).build());
        when(solicitacoes.findByIdForUpdate(30L)).thenReturn(Optional.of(solicitacao));
        assertThatThrownBy(() -> service.definirDestinoFinal(30L,DestinoProduto.ESTOQUE)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(destinos);
    }
    @Test void auditaQuemRealmenteDefiniuDestino() {
        var funcionario = Funcionario.builder().loja(loja).matricula("F2").build();
        when(identidade.funcionario()).thenReturn(funcionario);
        when(solicitacoes.findByIdForUpdate(30L)).thenReturn(Optional.of(solicitacao));
        solicitacao.setStatus(StatusSolicitacao.EM_ANALISE);
        solicitacao.setFuncionarioResponsavel(Funcionario.builder().matricula("F1").build());
        when(destinos.resolver(DestinoProduto.ESTOQUE)).thenReturn(mock(DestinoStrategy.class));
        service.definirDestinoFinal(30L,DestinoProduto.ESTOQUE);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.DESTINO_PENDENTE);
        assertThat(solicitacao.getDataConclusao()).isNull();
        verify(auditoria).save(argThat(a -> "F2".equals(a.getAutor())));
    }
}
