package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroPedido;

    @Column(nullable = false)
    private LocalDateTime dataCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ItemComprado> itens = new ArrayList<>();

    /**
     * Regra de elegibilidade a nivel de pedido (ex: pedido nao foi
     * cancelado/estornado). A elegibilidade por prazo fica em ItemComprado,
     * pois cada item pode ter uma data limite diferente.
     */
    public boolean isElegivelParaDevolucao(java.time.LocalDate hoje) {
        return itens.stream().anyMatch(item -> item.dentroDoPrazoDevolucao(hoje));
    }
}
