package com.magalu.devolucao.dto;
import java.time.LocalDateTime;
public record EventoClienteResponse(Long id, String tipo, String mensagem, LocalDateTime ocorridoEm) {}
