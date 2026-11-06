package com.magalu.devolucao.service.strategy;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import org.springframework.stereotype.Component;

@Component
public class EstoqueDestinoStrategy implements DestinoStrategy {

    @Override
    public DestinoProduto destinoSuportado() {
        return DestinoProduto.ESTOQUE;
    }

    @Override
    public void processar(SolicitacaoDevolucao solicitacao) {
        // TODO: integrar com sistema de estoque (dar entrada do item de volta).
        // O servico registra DESTINO_PENDENTE; nao confirma entrada em estoque.
    }
}
