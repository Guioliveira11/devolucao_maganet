package com.magalu.devolucao.dto;
import java.time.*;
public record AcompanhamentoResponse(Long id, String numeroPedido, String produto, int quantidade,
        Long lojaId, String loja, String enderecoLoja, LocalDate prazoSolicitacao,
        String status, String destino, Long ongDestinatariaId, LocalDateTime criadaEm,
        LocalDateTime recebidaEm, LocalDateTime destinoDefinidoEm, LocalDateTime concluidaEm,
        LocalDateTime qrExpiraEm) {}
