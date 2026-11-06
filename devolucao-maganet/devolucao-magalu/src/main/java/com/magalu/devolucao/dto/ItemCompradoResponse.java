package com.magalu.devolucao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemCompradoResponse(Long id, String numeroPedido, String produto,
        int quantidade, BigDecimal valorPago, LocalDate dataLimiteDevolucao,
        boolean elegivelParaDevolucao) {}
