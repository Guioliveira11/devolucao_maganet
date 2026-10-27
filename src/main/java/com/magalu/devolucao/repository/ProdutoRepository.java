package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @org.springframework.data.jpa.repository.Query(
            "select distinct p.categoria from Produto p where p.categoria is not null order by p.categoria")
    java.util.List<String> findCategorias();
}
