package com.magalu.devolucao.exception;

public class QRCodeInvalidoException extends RuntimeException {
    public QRCodeInvalidoException(String message) {
        super(message);
    }
}
