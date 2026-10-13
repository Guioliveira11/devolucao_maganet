package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;

import java.time.LocalDateTime;

public record SolicitacaoResponse(
        Long id,
        StatusSolicitacao status,
        DestinoProduto destinoFinal,
        String qrCodeImagemBase64,
        Long ongDestinatariaId,
        LocalDateTime dataDefinicaoDestino,
        LocalDateTime qrCodeExpiraEm
) {
    public static SolicitacaoResponse from(SolicitacaoDevolucao s, LocalDateTime agora) {
        var qr = s.qrCodeAtivo(agora);
        return new SolicitacaoResponse(
                s.getId(),
                s.getStatus(),
                s.getDestinoFinal(),
                qr != null ? qr.getImagemBase64() : null,
                s.getOngDestinataria() != null ? s.getOngDestinataria().getId() : null,
                s.getDataDefinicaoDestino(),
                qr != null ? qr.getDataExpiracao() : null
        );
    }
}
