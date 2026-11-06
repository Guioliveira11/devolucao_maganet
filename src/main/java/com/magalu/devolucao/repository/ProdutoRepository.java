package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
