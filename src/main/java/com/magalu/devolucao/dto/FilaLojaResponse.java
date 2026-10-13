package com.magalu.devolucao.dto;
import java.time.LocalDateTime;
public record FilaLojaResponse(Long id, String numeroPedido, String produto, int quantidade,
        String status, String destino, Long ongDestinatariaId, LocalDateTime recebidaEm,
        Long minutosDesdeRecebimento) {}
