package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface SolicitacaoDevolucaoRepository extends JpaRepository<SolicitacaoDevolucao, Long> {
    @Query("select s.itemComprado.id from SolicitacaoDevolucao s where s.itemComprado.pedido.usuario.id = :usuarioId and s.itemComprado.id in :itens and s.status <> com.magalu.devolucao.domain.enums.StatusSolicitacao.CANCELADA")
    Set<Long> findItensBloqueados(@Param("usuarioId") Long usuarioId, @Param("itens") Collection<Long> itens);
    String VENCIDAS = """
        FROM solicitacoes_devolucao s
        WHERE s.status='AGUARDANDO_ESCANEAMENTO'
          AND EXISTS (SELECT 1 FROM qrcodes_devolucao q WHERE q.solicitacao_id=s.id
                      AND q.status='ATIVO' AND q.data_expiracao <= :agora)
          AND NOT EXISTS (SELECT 1 FROM qrcodes_devolucao q WHERE q.solicitacao_id=s.id
                          AND q.status='ATIVO' AND q.data_expiracao > :agora)
        """;

    @Query(value="SELECT s.* " + VENCIDAS + " ORDER BY s.id LIMIT :limite FOR UPDATE OF s SKIP LOCKED", nativeQuery=true)
    List<SolicitacaoDevolucao> findVencidasParaAtualizacao(@Param("agora") LocalDateTime agora, @Param("limite") int limite);

    @Query(value="SELECT count(*) " + VENCIDAS, nativeQuery=true)
    long countVencidas(@Param("agora") LocalDateTime agora);

    List<SolicitacaoDevolucao> findByStatus(StatusSolicitacao status);
    boolean existsByItemCompradoIdAndStatusNot(Long itemId, StatusSolicitacao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoDevolucao s where s.id = :id")
    Optional<SolicitacaoDevolucao> findByIdForUpdate(@Param("id") Long id);

    @Query("select s.id from SolicitacaoDevolucao s where s.status = :status " +
           "and exists (select q.id from QRCodeDevolucao q where q.solicitacao = s " +
           "and q.status = com.magalu.devolucao.domain.enums.StatusQRCode.ATIVO and q.dataExpiracao <= :agora)")
    List<Long> findIdsComQrVencido(@Param("status") StatusSolicitacao status,
                                  @Param("agora") LocalDateTime agora);

    @Query("select s.id from SolicitacaoDevolucao s join s.historicoQrCodes q where q.token = :token")
    Optional<Long> findIdByQrToken(@Param("token") UUID token);

    org.springframework.data.domain.Page<SolicitacaoDevolucao> findByOngDestinataria_IdAndDestinoFinal(
            Long ongId, com.magalu.devolucao.domain.enums.DestinoProduto destino,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<SolicitacaoDevolucao> findByOngDestinataria_IdAndDestinoFinalAndStatus(
            Long ongId, com.magalu.devolucao.domain.enums.DestinoProduto destino, StatusSolicitacao status,
            org.springframework.data.domain.Pageable pageable);
}
