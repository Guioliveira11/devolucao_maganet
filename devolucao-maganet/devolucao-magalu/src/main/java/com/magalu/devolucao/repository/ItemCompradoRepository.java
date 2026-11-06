package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.ItemComprado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCompradoRepository extends JpaRepository<ItemComprado, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemComprado i where i.id = :id")
    Optional<ItemComprado> findByIdForUpdate(@Param("id") Long id);

    // Usada na tela inicial: "meus itens elegiveis para devolucao"
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"pedido", "produto"})
    org.springframework.data.domain.Page<ItemComprado> findByPedido_Usuario_Id(
            Long usuarioId, org.springframework.data.domain.Pageable pageable);
}
