package com.magalu.devolucao.exception;
public class FalhaGeracaoQRCodeException extends RuntimeException {
    public FalhaGeracaoQRCodeException(Throwable causa) { super("Falha ao gerar imagem do QR Code", causa); }
}
