package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ongs_parceiras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OngParceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @ElementCollection
    @CollectionTable(name = "ong_categorias_aceitas", joinColumns = @JoinColumn(name = "ong_id"))
    @Column(name = "categoria")
    @Builder.Default
    private List<String> categoriasAceitas = new ArrayList<>();

    @Builder.Default
    private boolean ativa = true;
}
