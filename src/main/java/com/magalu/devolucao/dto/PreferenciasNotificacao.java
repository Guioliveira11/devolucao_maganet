package com.magalu.devolucao.dto;
import jakarta.validation.constraints.NotNull;
public record PreferenciasNotificacao(@NotNull Boolean atualizacoes, @NotNull Boolean lembretesQr) {}
