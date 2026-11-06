package com.magalu.devolucao.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarTokenRequest(@NotBlank String token) {}
