package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Long> {

    Optional<RedefinicaoSenha> findByTokenHash(String tokenHash);

    long countByConta_IdAndCriadoEmAfter(Long contaId, LocalDateTime desde);

    /** Invalida os links ainda nao usados da conta (so o mais recente deve valer). */
    @Modifying
    @Query("update RedefinicaoSenha r set r.usadoEm = :agora where r.conta.id = :contaId and r.usadoEm is null")
    int invalidarPendentes(@Param("contaId") Long contaId, @Param("agora") LocalDateTime agora);

    /** Consome o token de forma atomica: retorna 1 so para quem chegou primeiro. */
    @Modifying
    @Query("update RedefinicaoSenha r set r.usadoEm = :agora "
            + "where r.tokenHash = :hash and r.usadoEm is null and r.expiraEm > :agora")
    int consumir(@Param("hash") String hash, @Param("agora") LocalDateTime agora);
}