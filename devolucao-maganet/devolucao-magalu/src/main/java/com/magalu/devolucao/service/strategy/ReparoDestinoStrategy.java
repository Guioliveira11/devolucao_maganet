package com.magalu.devolucao.service.strategy;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import org.springframework.stereotype.Component;

@Component
public class ReparoDestinoStrategy implements DestinoStrategy {

    @Override
    public DestinoProduto destinoSuportado() {
        return DestinoProduto.REPARO;
    }

    @Override
    public void processar(SolicitacaoDevolucao solicitacao) {
        boolean elegivel = solicitacao.getItemComprado().getProduto().isElegivelReparo();
        if (!elegivel) {
            throw new TransicaoInvalidaException(
                    "Produto " + solicitacao.getItemComprado().getProduto().getNome() +
                    " nao e elegivel para reparo");
        }
        // TODO: integrar com sistema de assistencia tecnica / abrir OS.
    }
}
