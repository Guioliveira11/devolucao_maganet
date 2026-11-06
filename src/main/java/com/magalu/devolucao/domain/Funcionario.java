package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "funcionarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String matricula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id")
    private Loja loja;

    // As acoes de "escanear" e "definir destino" ficam no DevolucaoService,
    // que recebe o Funcionario como parametro. Manter a entidade "anemica"
    // aqui evita acoplar regra de negocio de outras entidades (SolicitacaoDevolucao)
    // dentro do Funcionario.
}
