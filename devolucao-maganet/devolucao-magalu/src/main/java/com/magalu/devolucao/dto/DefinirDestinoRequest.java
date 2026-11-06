package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.ItemConferencia;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Aprovacao da conferencia: destino do produto e itens conferidos no balcao. */
public record DefinirDestinoRequest(
        @NotNull DestinoProduto destino,
        List<ItemConferencia> itensConferidos
) {
    public DefinirDestinoRequest(DestinoProduto destino) {
        this(destino, List.of());
    }
}
