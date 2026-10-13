package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.enums.DestinoProduto;
import jakarta.validation.constraints.NotNull;

public record DefinirDestinoRequest(
        @NotNull DestinoProduto destino
) {}
