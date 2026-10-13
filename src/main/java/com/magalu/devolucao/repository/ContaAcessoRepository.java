package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.ContaAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {
    Optional<ContaAcesso> findByLogin(String login);
}
