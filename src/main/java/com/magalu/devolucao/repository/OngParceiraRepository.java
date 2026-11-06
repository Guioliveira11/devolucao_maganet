package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.OngParceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OngParceiraRepository extends JpaRepository<OngParceira, Long> {
    List<OngParceira> findByAtivaTrueAndCategoriasAceitasContaining(String categoria);
}
