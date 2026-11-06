package com.magalu.devolucao.service.strategy;

import com.magalu.devolucao.domain.OngParceira;
import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import com.magalu.devolucao.repository.OngParceiraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DoacaoOngDestinoStrategy implements DestinoStrategy {

    private final OngParceiraRepository ongParceiraRepository;

    @Override
    public DestinoProduto destinoSuportado() {
        return DestinoProduto.DOACAO_ONG;
    }

    @Override
    public void processar(SolicitacaoDevolucao solicitacao) {
        String categoria = solicitacao.getItemComprado().getProduto().getCategoria();
        List<OngParceira> ongsCompativeis =
                ongParceiraRepository.findByAtivaTrueAndCategoriasAceitasContaining(categoria);

        if (ongsCompativeis.isEmpty()) {
            throw new TransicaoInvalidaException(
                    "Nenhuma ONG parceira ativa aceita a categoria: " + categoria);
        }
        // Escolha deterministica; a coleta permanece pendente de integracao.
        solicitacao.setOngDestinataria(ongsCompativeis.stream()
                .min(java.util.Comparator.comparing(OngParceira::getId)).orElseThrow());
    }
}
