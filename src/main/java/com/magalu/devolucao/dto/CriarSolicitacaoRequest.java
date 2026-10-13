package com.magalu.devolucao.dto;

import jakarta.validation.constraints.*;

public record CriarSolicitacaoRequest(
        @NotNull @Positive Long itemCompradoId,
        @NotNull @Positive Long lojaId
) {}
