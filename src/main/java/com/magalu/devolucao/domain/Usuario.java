package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, unique = true)
    private String email;

    private String telefone;

    // Localizacao informada no momento da busca por lojas proximas.
    // Nao e necessariamente persistida com frequencia; pode vir apenas via DTO
    // de requisicao. Mantida aqui caso se opte por cache da ultima localizacao.
    private Double latitudeAtual;
    private Double longitudeAtual;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();
}
