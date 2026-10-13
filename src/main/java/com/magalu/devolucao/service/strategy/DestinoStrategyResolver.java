package com.magalu.devolucao.service.strategy;

import com.magalu.devolucao.domain.enums.DestinoProduto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O Spring injeta automaticamente TODAS as implementacoes de DestinoStrategy
 * aqui (List<DestinoStrategy>). Isso significa que adicionar um novo destino
 * no futuro (ex: RECICLAGEM) exige apenas criar a classe @Component nova —
 * nada muda neste resolver nem no DevolucaoService.
 */
@Component
public class DestinoStrategyResolver {

    private final Map<DestinoProduto, DestinoStrategy> estrategiasPorDestino;

    public DestinoStrategyResolver(List<DestinoStrategy> estrategias) {
        this.estrategiasPorDestino = estrategias.stream()
                .collect(Collectors.toMap(DestinoStrategy::destinoSuportado, Function.identity()));
    }

    public DestinoStrategy resolver(DestinoProduto destino) {
        DestinoStrategy estrategia = estrategiasPorDestino.get(destino);
        if (estrategia == null) {
            throw new IllegalStateException("Nenhuma estrategia registrada para o destino: " + destino);
        }
        return estrategia;
    }
}
