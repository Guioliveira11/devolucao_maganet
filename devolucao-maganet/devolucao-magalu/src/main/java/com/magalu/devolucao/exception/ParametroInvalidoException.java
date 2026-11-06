package com.magalu.devolucao.exception;
/** Parametro de entrada explicitamente validado pela aplicacao. */
public class ParametroInvalidoException extends IllegalArgumentException {
    public ParametroInvalidoException(String mensagem) { super(mensagem); }
}
