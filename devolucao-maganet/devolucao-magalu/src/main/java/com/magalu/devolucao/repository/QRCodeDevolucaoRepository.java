package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.QRCodeDevolucao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QRCodeDevolucaoRepository extends JpaRepository<QRCodeDevolucao, Long> {
    Optional<QRCodeDevolucao> findByToken(UUID token);
}
