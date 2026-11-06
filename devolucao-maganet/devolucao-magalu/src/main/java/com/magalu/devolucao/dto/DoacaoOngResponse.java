package com.magalu.devolucao.dto;

import java.time.LocalDateTime;

/** Doacao encaminhada a uma ONG. Nunca inclui dados pessoais do cliente. */
public record DoacaoOngResponse(Long id, String numeroPedido, String produto, String categoria, int quantidade,
                                String loja, String status, LocalDateTime definidaEm, LocalDateTime concluidaEm) {}
