package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.ItemConferencia;
import com.magalu.devolucao.domain.enums.MotivoReprovacao;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa a regra mais critica do dominio: nao pode haver salto de estado
 * fora do fluxo definido (ex: concluir sem antes escanear o QR).
 */
class SolicitacaoDevolucaoTest {

    @Test
    void naoDevePermitirConcluirSemPassarPorEmAnalise() {
        SolicitacaoDevolucao solicitacao = SolicitacaoDevolucao.builder()
                .status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO)
                .build();

        assertThatThrownBy(() -> solicitacao.concluir(LocalDateTime.of(2026, 9, 19, 12, 0)))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void devePermitirFluxoFelizCompleto() {
        SolicitacaoDevolucao solicitacao = SolicitacaoDevolucao.builder()
                .status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO)
                .build();

        solicitacao.confirmarEscaneamento(null, LocalDateTime.of(2026, 9, 19, 12, 0));
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.EM_ANALISE);

        solicitacao.registrarConferencia(
                List.of(ItemConferencia.CORRESPONDE_AO_PEDIDO, ItemConferencia.SEM_SINAIS_DE_USO),
                LocalDateTime.of(2026, 9, 19, 12, 0)
        );
        solicitacao.definirDestino(DestinoProduto.REPARO, LocalDateTime.of(2026, 9, 19, 12, 0));
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.DESTINO_PENDENTE);
        assertThat(solicitacao.itensConferidosLista())
                .containsExactlyInAnyOrder(ItemConferencia.CORRESPONDE_AO_PEDIDO, ItemConferencia.SEM_SINAIS_DE_USO);
        assertThat(solicitacao.getDataConclusao()).isNull();

        solicitacao.concluir(LocalDateTime.of(2026, 9, 20, 12, 0));
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
        assertThat(solicitacao.getDestinoFinal()).isEqualTo(DestinoProduto.REPARO);
        assertThat(solicitacao.getDataConclusao()).isNotNull();
    }

    @Test
    void devePermitirReprovarNaConferenciaComoEstadoFinal() {
        SolicitacaoDevolucao solicitacao = SolicitacaoDevolucao.builder()
                .status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO)
                .build();

        solicitacao.confirmarEscaneamento(null, LocalDateTime.of(2026, 9, 19, 12, 0));
        solicitacao.reprovar(MotivoReprovacao.SINAIS_DE_USO, "Marcas de uso na peca", LocalDateTime.of(2026, 9, 19, 12, 10));

        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.REPROVADA);
        assertThat(solicitacao.getMotivoReprovacao()).isEqualTo(MotivoReprovacao.SINAIS_DE_USO);
        assertThat(solicitacao.getDataConclusao()).isNotNull();
        assertThatThrownBy(() -> solicitacao.definirDestino(DestinoProduto.ESTOQUE, LocalDateTime.of(2026, 9, 19, 12, 20)))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void naoDevePermitirTransicaoAPartirDeEstadoFinal() {
        SolicitacaoDevolucao solicitacao = SolicitacaoDevolucao.builder()
                .status(StatusSolicitacao.CONCLUIDA)
                .build();

        assertThatThrownBy(() -> solicitacao.transicionarEstado(StatusSolicitacao.EM_ANALISE))
                .isInstanceOf(TransicaoInvalidaException.class);
    }
}
