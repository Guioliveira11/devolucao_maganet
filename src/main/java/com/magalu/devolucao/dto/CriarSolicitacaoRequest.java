package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.enums.MotivoDevolucao;
import com.magalu.devolucao.domain.enums.TipoResolucao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido de devolucao montado nas etapas do app: motivo, fotos, solucao e loja.
 * As fotos chegam como data URL ("data:image/jpeg;base64,...") ja comprimidas no celular.
 */
public record CriarSolicitacaoRequest(
        @NotNull @Positive Long itemCompradoId,
        @NotNull @Positive Long lojaId,
        @NotNull MotivoDevolucao motivo,
        @Size(max = 500) String comentario,
        @NotNull TipoResolucao tipoResolucao,
        @Size(max = 3) List<String> evidencias
) {
    public CriarSolicitacaoRequest(Long itemCompradoId, Long lojaId) {
        this(itemCompradoId, lojaId, null, null, null, List.of());
    }
}
