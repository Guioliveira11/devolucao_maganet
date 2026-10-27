package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.Loja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LojaRepository extends JpaRepository<Loja, Long> {
    org.springframework.data.domain.Page<Loja> findByAtivaTrueAndNomeContainingIgnoreCaseOrAtivaTrueAndEnderecoContainingIgnoreCase(
            String nome, String endereco, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Query(value = """
        SELECT l.* FROM lojas l
        WHERE l.ativa = true AND
        6371.0 * 2 * asin(sqrt(least(1.0, greatest(0.0,
            power(sin(radians(l.latitude - :latitude) / 2), 2) +
            cos(radians(:latitude)) * cos(radians(l.latitude)) *
            power(sin(radians(l.longitude - :longitude) / 2), 2)
        )))) <= :raioKm
        """, nativeQuery = true)
    List<Loja> findProximas(@org.springframework.data.repository.query.Param("latitude") double latitude,
            @org.springframework.data.repository.query.Param("longitude") double longitude,
            @org.springframework.data.repository.query.Param("raioKm") double raioKm);

    List<Loja> findByAtivaTrueOrderByNomeAsc();
}
