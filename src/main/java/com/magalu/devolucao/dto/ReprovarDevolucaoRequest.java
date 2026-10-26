package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.enums.ItemConferencia;
import com.magalu.devolucao.domain.enums.MotivoReprovacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReprovarDevolucaoRequest(
        @NotNull MotivoReprovacao motivo,
        @Size(max = 500) String observacao,
        List<ItemConferencia> itensConferidos
) {}
