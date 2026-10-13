package com.magalu.devolucao.service.strategy;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;

/**
 * Cada implementacao encapsula a regra especifica de UM destino possivel
 * para o produto devolvido. O DevolucaoService escolhe a implementacao
 * correta via DestinoStrategyResolver e delega o processamento.
 */
public interface DestinoStrategy {

    DestinoProduto destinoSuportado();

    /**
     * Executa as acoes necessarias para esse destino (ex: dar entrada
     * no estoque, abrir OS de reparo, notificar ONG). Efeitos colaterais
     * (integracao com outros sistemas) ficam encapsulados aqui.
     */
    void processar(SolicitacaoDevolucao solicitacao);
}
