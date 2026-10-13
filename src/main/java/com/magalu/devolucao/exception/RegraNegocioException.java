package com.magalu.devolucao.exception;
/** Conflito esperado com o estado ou as regras da devolucao. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
