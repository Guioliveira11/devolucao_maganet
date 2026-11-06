package com.magalu.devolucao.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EscaneamentoRequest(@NotNull UUID token) {}
