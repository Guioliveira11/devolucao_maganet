package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.ContaAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {
    Optional<ContaAcesso> findByLogin(String login);

    @Query(value = """
            SELECT c.*
              FROM contas_acesso c
              LEFT JOIN usuarios u ON u.id = c.usuario_id
              LEFT JOIN funcionarios f ON f.id = c.funcionario_id
             WHERE lower(coalesce(u.email, f.email)) = lower(:email)
               AND c.ativa = true
             ORDER BY c.id
             LIMIT 1
            """, nativeQuery = true)
    Optional<ContaAcesso> findAtivaByEmail(@Param("email") String email);
}
