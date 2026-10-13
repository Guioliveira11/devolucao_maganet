package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lojas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String endereco;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String horarioFuncionamento;

    @Builder.Default
    private boolean ativa = true;

    private static final double RAIO_TERRA_KM = 6371.0;

    /**
     * Distancia em km ate um ponto (lat/lon), via formula de Haversine.
     * Usada pelo LojaProximaService para ordenar lojas por proximidade.
     */
    public double calcularDistancia(double latOrigem, double lonOrigem) {
        double dLat = Math.toRadians(this.latitude - latOrigem);
        double dLon = Math.toRadians(this.longitude - lonOrigem);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latOrigem)) * Math.cos(Math.toRadians(this.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        a = Math.max(0, Math.min(1, a));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAIO_TERRA_KM * c;
    }
}
