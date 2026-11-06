package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "itens_comprados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemComprado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    private int quantidade;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorPago;

    // Calculada na criacao do item (ex: dataCompra + 90 dias),
    // e o dado que a tela de "meus pedidos" usa para decidir
    // se mostra o botao de "solicitar devolucao".
    private LocalDate dataLimiteDevolucao;

    public boolean dentroDoPrazoDevolucao(LocalDate hoje) {
        return dataLimiteDevolucao != null && !hoje.isAfter(dataLimiteDevolucao);
    }
}
