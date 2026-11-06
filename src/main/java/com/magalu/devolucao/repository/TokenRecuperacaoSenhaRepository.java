package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {
    Optional<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRecuperacaoSenha t where t.tokenHash = :tokenHash")
    Optional<TokenRecuperacaoSenha> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    void deleteByContaId(Long contaId);
    void deleteByContaIdAndIdNot(Long contaId, Long id);
}
