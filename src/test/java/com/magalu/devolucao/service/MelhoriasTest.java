package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.repository.*;
import com.magalu.devolucao.service.strategy.DoacaoOngDestinoStrategy;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MelhoriasTest {
    @Test void selecionaOngDeterministicamenteESalvaDestinoPendente() {
        var repo = mock(OngParceiraRepository.class);
        var menor = OngParceira.builder().id(2L).build();
        when(repo.findByAtivaTrueAndCategoriasAceitasContaining("livros"))
                .thenReturn(List.of(OngParceira.builder().id(9L).build(), menor));
        var s = SolicitacaoDevolucao.builder().status(StatusSolicitacao.EM_ANALISE)
                .itemComprado(ItemComprado.builder().produto(Produto.builder().categoria("livros").build()).build()).build();
        new DoacaoOngDestinoStrategy(repo).processar(s);
        var agora = LocalDateTime.of(2026, 9, 19, 12, 0);
        s.definirDestino(DestinoProduto.DOACAO_ONG, agora);
        assertThat(s.getOngDestinataria()).isSameAs(menor);
        assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.DESTINO_PENDENTE);
        assertThat(s.getDataDefinicaoDestino()).isEqualTo(agora);
        assertThat(s.getDataConclusao()).isNull();
    }

    @Test void rejeitaDoacaoSemDestinataria() {
        var s = SolicitacaoDevolucao.builder().status(StatusSolicitacao.EM_ANALISE).build();
        assertThatThrownBy(() -> s.definirDestino(DestinoProduto.DOACAO_ONG, LocalDateTime.of(2026,9,19,12,0)))
                .hasMessageContaining("ONG");
        assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.EM_ANALISE);
    }

    @Test void rejeitaCoordenadasERaiosInvalidosAntesDeConsultarBanco() {
        var repo = mock(LojaRepository.class);
        var service = new LojaProximaService(repo);
        double[][] entradas = {{91,0,20},{-91,0,20},{0,181,20},{0,-181,20},
                {Double.NaN,0,20},{0,Double.POSITIVE_INFINITY,20},{0,0,Double.NaN},
                {0,0,Double.POSITIVE_INFINITY},{0,0,0},{0,0,-1},{0,0,201}};
        for (var e : entradas)
            assertThatThrownBy(() -> service.buscarLojasProximas(e[0],e[1],e[2]))
                    .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test void consultaFiltradaOrdenaPorDistancia() {
        var repo = mock(LojaRepository.class);
        var perto = Loja.builder().id(1L).latitude(0.0).longitude(0.0).build();
        var longe = Loja.builder().id(2L).latitude(0.1).longitude(0.0).build();
        when(repo.findProximas(0,0,20)).thenReturn(List.of(longe, perto));
        assertThat(new LojaProximaService(repo).buscarLojasProximas(0,0,20)).containsExactly(perto,longe);
    }

    @Test void prazoIncluiUltimoDiaEQrExpiraNoInstanteExato() {
        var clock = Clock.fixed(Instant.parse("2026-09-20T02:59:59Z"), ZoneId.of("America/Sao_Paulo"));
        var item = ItemComprado.builder().dataLimiteDevolucao(LocalDate.of(2026,9,19)).build();
        assertThat(item.dentroDoPrazoDevolucao(LocalDate.now(clock))).isTrue();
        assertThat(item.dentroDoPrazoDevolucao(LocalDate.now(Clock.offset(clock,Duration.ofSeconds(1))))).isFalse();
        var qr = QRCodeDevolucao.builder().status(StatusQRCode.ATIVO).dataExpiracao(LocalDateTime.now(clock)).build();
        assertThat(qr.isValido(LocalDateTime.now(clock).minusNanos(1))).isTrue();
        assertThat(qr.isValido(LocalDateTime.now(clock))).isFalse();
    }
}
