package com.magalu.devolucao.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcompanhamentoResponse(
        Long id,
        String numeroPedido,
        String produto,
        int quantidade,
        Long lojaId,
        String loja,
        String enderecoLoja,
        LocalDate prazoSolicitacao,
        String status,
        String destino,
        Long ongDestinatariaId,
        LocalDateTime criadaEm,
        LocalDateTime recebidaEm,
        LocalDateTime destinoDefinidoEm,
        LocalDateTime concluidaEm,
        LocalDateTime qrExpiraEm,
        String motivo,
        String tipoResolucao,
        String motivoReprovacao,
        String observacaoConferencia
) {}
