package com.magalu.devolucao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@lombok.extern.slf4j.Slf4j
@lombok.RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler extends org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler {
    private final java.time.Clock clock;

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleParametros(ParametroInvalidoException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(QRCodeInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleQrInvalido(QRCodeInvalidoException ex) {
        return erro(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicaoInvalida(TransicaoInvalidaException ex) {
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcesso(RuntimeException ex) {
        return erro(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegra(RegraNegocioException ex) {
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(RuntimeException ex) {
        return erro(HttpStatus.CONFLICT, "Operacao em conflito com os dados existentes");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleFalhaInterna(RuntimeException ex) {
        log.error("Falha interna ao processar requisicao", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a solicitacao.");
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now(clock),
                "status", status.value(),
                "mensagem", mensagem
        ));
    }
}
