package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String nome;

    private String categoria;

    // Regra de negocio simples embutida na entidade: define se esse
    // MODELO de produto e passivel de reparo. A elegibilidade final da
    // devolucao especifica ainda depende do prazo (ver ItemComprado).
    private boolean elegivelReparo;
}
