package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @Query("select distinct p.categoria from Produto p where p.categoria is not null order by p.categoria")
    List<String> findCategorias();
}
