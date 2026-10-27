package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);
}
