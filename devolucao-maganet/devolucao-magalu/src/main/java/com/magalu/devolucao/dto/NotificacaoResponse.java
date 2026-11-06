package com.magalu.devolucao.dto;
import java.time.LocalDateTime;
public record NotificacaoResponse(Long id, Long solicitacaoId, String tipo, String mensagem,
        LocalDateTime criadaEm, LocalDateTime lidaEm, LocalDateTime qrExpiraEm) {}
