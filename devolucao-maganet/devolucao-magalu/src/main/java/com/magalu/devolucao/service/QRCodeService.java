package com.magalu.devolucao.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.magalu.devolucao.domain.QRCodeDevolucao;
import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.StatusQRCode;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.exception.QRCodeInvalidoException;
import com.magalu.devolucao.repository.SolicitacaoDevolucaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRCodeService {
    private final java.time.Clock clock;
    private final com.magalu.devolucao.repository.RegistroAuditoriaRepository auditoria;
    private final NotificacaoService notificacoes;

    private final SolicitacaoDevolucaoRepository solicitacaoRepository;

    @Value("${app.qrcode.validade-horas:24}")
    private int validadeHoras;

    @Value("${app.qrcode.tamanho-px:300}")
    private int tamanhoPx;

    /**
     * Gera um novo QR Code para a solicitacao. Se ja existir um QR ativo
     * (nao expirado e nao utilizado), reaproveita ao inves de criar outro â€”
     * evita multiplos codigos validos simultaneos para a mesma solicitacao.
     */
    public QRCodeDevolucao gerarQRCode(SolicitacaoDevolucao solicitacao) {
        LocalDateTime agora = LocalDateTime.now(clock);
        QRCodeDevolucao ativo = solicitacao.qrCodeAtivo(agora);
        if (ativo != null) {
            return ativo;
        }

        // Encerra codigos antigos antes de emitir um novo.
        solicitacao.getHistoricoQrCodes().stream().filter(q -> q.isExpirado(agora))
                .forEach(q -> q.invalidar(agora));
        UUID token = UUID.randomUUID();

        QRCodeDevolucao novoQr = QRCodeDevolucao.builder()
                .token(token)
                .imagemBase64(gerarImagemBase64(token))
                .dataGeracao(agora)
                .dataExpiracao(agora.plusHours(validadeHoras))
                .status(StatusQRCode.ATIVO)
                .build();

        solicitacao.adicionarQrCode(novoQr);
        if (solicitacao.getStatus() == StatusSolicitacao.QR_EXPIRADO) {
            solicitacao.transicionarEstado(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        }

        return novoQr;
    }

    /**
     * Valida o token lido pelo funcionario. Lanca excecao se o QR nao
     * existir, ja tiver sido usado ou estiver expirado â€” o controller
     * traduz isso em uma resposta HTTP adequada (ver GlobalExceptionHandler).
     */
    public SolicitacaoDevolucao validarQRCode(UUID token) {
        SolicitacaoDevolucao solicitacao = solicitacaoRepository.findIdByQrToken(token)
                .flatMap(solicitacaoRepository::findByIdForUpdate)
                .orElseThrow(() -> new QRCodeInvalidoException("QR Code nao encontrado"));

        QRCodeDevolucao qr = solicitacao.qrCodeAtivo(LocalDateTime.now(clock));
        if (qr == null || !qr.getToken().equals(token)) {
            throw new QRCodeInvalidoException("QR Code expirado ou ja utilizado. Gere um novo.");
        }

        return solicitacao;
    }

    /**
     * Job periodico (chamado por um @Scheduled em outra classe de
     * infraestrutura, ex: QRCodeExpirationJob) que varre solicitacoes
     * aguardando escaneamento cujo QR passou das 24h e move o status
     * para QR_EXPIRADO.
     */
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public ResultadoLote expirarCodigosVencidos(int tamanhoLote) {
        ResultadoLote.validarTamanho(tamanhoLote);
        var vencidas = solicitacaoRepository.findVencidasParaAtualizacao(LocalDateTime.now(clock), tamanhoLote);
        int expiradas = 0;
        for (var solicitacao : vencidas) {
            if (solicitacao.getStatus() != StatusSolicitacao.AGUARDANDO_ESCANEAMENTO) continue;
            var agora = LocalDateTime.now(clock);
            if (solicitacao.qrCodeAtivo(agora) != null) continue;
            solicitacao.getHistoricoQrCodes().stream().filter(q -> q.isExpirado(agora))
                    .forEach(q -> q.invalidar(agora));
            solicitacao.transicionarEstado(StatusSolicitacao.QR_EXPIRADO);
            var evento = auditoria.save(com.magalu.devolucao.domain.RegistroAuditoria.builder()
                    .solicitacao(solicitacao).autor("SISTEMA").acao("QR_EXPIRADO").timestamp(agora).build());
            notificacoes.registrarEvento(evento);
            expiradas++;
        }
        return new ResultadoLote(vencidas.size(), expiradas);
    }

    @Transactional(readOnly=true)
    public long contarExpiracoesPendentes() {
        return solicitacaoRepository.countVencidas(LocalDateTime.now(clock));
    }

    private String gerarImagemBase64(UUID token) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(token.toString(), BarcodeFormat.QR_CODE, tamanhoPx, tamanhoPx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException | IllegalArgumentException e) {
            throw new com.magalu.devolucao.exception.FalhaGeracaoQRCodeException(e);
        }
    }
}
