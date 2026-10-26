package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.EvidenciaDevolucao;
import com.magalu.devolucao.domain.SolicitacaoDevolucao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Tudo que o funcionario precisa ver para conferir o produto no balcao. */
public record ConferenciaResponse(
        Long id,
        String numeroPedido,
        String produto,
        String categoria,
        int quantidade,
        BigDecimal valorPago,
        String status,
        String motivo,
        String comentario,
        String tipoResolucao,
        LocalDateTime criadaEm,
        LocalDateTime recebidaEm,
        List<String> itensConferidos,
        String destino,
        String motivoReprovacao,
        String observacaoConferencia,
        List<String> evidencias
) {
    public static ConferenciaResponse from(SolicitacaoDevolucao s) {
        var item = s.getItemComprado();
        return new ConferenciaResponse(
                s.getId(),
                item.getPedido().getNumeroPedido(),
                item.getProduto().getNome(),
                item.getProduto().getCategoria(),
                item.getQuantidade(),
                item.getValorPago(),
                s.getStatus().name(),
                nome(s.getMotivo()),
                s.getComentario(),
                nome(s.getTipoResolucao()),
                s.getDataCriacao(),
                s.getDataRecebimento(),
                s.itensConferidosLista().stream().map(Enum::name).toList(),
                nome(s.getDestinoFinal()),
                nome(s.getMotivoReprovacao()),
                s.getObservacaoConferencia(),
                s.getEvidencias().stream().map(EvidenciaDevolucao::comoDataUrl).toList()
        );
    }

    private static String nome(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}
