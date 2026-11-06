package com.magalu.devolucao.repository;

import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.EventoCliente;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.dto.AcompanhamentoResponse;
import com.magalu.devolucao.dto.EventoClienteResponse;
import com.magalu.devolucao.dto.FilaLojaResponse;
import com.magalu.devolucao.dto.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PortalRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String JOINS = """
        FROM solicitacoes_devolucao s
        JOIN itens_comprados i ON i.id = s.item_comprado_id
        JOIN pedidos p ON p.id = i.pedido_id
        JOIN produtos pr ON pr.id = i.produto_id
        JOIN lojas l ON l.id = s.loja_id
        """;

    private static final String COLUNAS = """
        SELECT s.*, p.numero_pedido, pr.nome AS produto, i.quantidade,
        i.data_limite_devolucao, l.nome AS loja, l.endereco,
        (SELECT max(q.data_expiracao) FROM qrcodes_devolucao q
         WHERE q.solicitacao_id = s.id AND q.status = 'ATIVO' AND q.data_expiracao > :agora) AS qr_expira_em
        """;

    public PaginaResponse<AcompanhamentoResponse> listarCliente(
            long usuarioId, StatusSolicitacao status, int pagina, int tamanho, LocalDateTime agora) {
        var params = parametros(pagina, tamanho);
        params.put("usuario", usuarioId);
        params.put("agora", agora);

        String where = " WHERE s.usuario_id = :usuario";
        if (status != null) {
            where += " AND s.status = :status";
            params.put("status", status.name());
        }

        long total = jdbc.queryForObject("SELECT count(*) FROM solicitacoes_devolucao s" + where, params, Long.class);
        var itens = jdbc.query(
                COLUNAS + JOINS + where + " ORDER BY s.data_criacao DESC, s.id DESC LIMIT :limite OFFSET :offset",
                params,
                (rs, n) -> acompanhamento(rs)
        );
        return new PaginaResponse<>(itens, pagina, tamanho, total);
    }

    public Optional<AcompanhamentoResponse> detalheCliente(long usuarioId, long id, LocalDateTime agora) {
        return jdbc.query(
                COLUNAS + JOINS + " WHERE s.usuario_id = :usuario AND s.id = :id",
                Map.of("usuario", usuarioId, "id", id, "agora", agora),
                (rs, n) -> acompanhamento(rs)
        ).stream().findFirst();
    }

    public PaginaResponse<EventoClienteResponse> historico(long usuarioId, long id, int pagina, int tamanho) {
        var params = parametros(pagina, tamanho);
        params.put("usuario", usuarioId);
        params.put("id", id);
        params.put("acoes", Arrays.stream(EventoCliente.values()).map(Enum::name).toList());

        String from = """
             FROM registros_auditoria a
             JOIN solicitacoes_devolucao s ON s.id = a.solicitacao_id
             WHERE s.usuario_id = :usuario AND s.id = :id AND a.acao IN (:acoes)
            """;

        long total = jdbc.queryForObject("SELECT count(*)" + from, params, Long.class);
        var itens = jdbc.query(
                "SELECT a.id, a.acao, a.timestamp" + from + " ORDER BY a.timestamp DESC, a.id DESC LIMIT :limite OFFSET :offset",
                params,
                (rs, n) -> new EventoClienteResponse(
                        rs.getLong("id"),
                        rs.getString("acao"),
                        EventoCliente.valueOf(rs.getString("acao")).mensagem(),
                        hora(rs, "timestamp")
                )
        );
        return new PaginaResponse<>(itens, pagina, tamanho, total);
    }

    public PaginaResponse<FilaLojaResponse> fila(
            long lojaId, StatusSolicitacao status, DestinoProduto destino,
            Long minimoHoras, int pagina, int tamanho, LocalDateTime agora) {
        var params = parametros(pagina, tamanho);
        params.put("loja", lojaId);

        String where = " WHERE s.loja_id = :loja AND s.status IN ('EM_ANALISE', 'DESTINO_PENDENTE')";
        if (status != null) {
            where += " AND s.status = :status";
            params.put("status", status.name());
        }
        if (destino != null) {
            where += " AND s.destino_final = :destino";
            params.put("destino", destino.name());
        }
        if (minimoHoras != null) {
            where += " AND s.data_recebimento <= :limiteTempo";
            params.put("limiteTempo", agora.minusHours(minimoHoras));
        }

        long total = jdbc.queryForObject("SELECT count(*) FROM solicitacoes_devolucao s" + where, params, Long.class);
        var itens = jdbc.query(
                "SELECT s.*, p.numero_pedido, pr.nome AS produto, i.quantidade " + JOINS + where
                        + " ORDER BY s.data_recebimento ASC NULLS LAST, s.id ASC LIMIT :limite OFFSET :offset",
                params,
                (rs, n) -> {
                    var recebida = hora(rs, "data_recebimento");
                    Long minutos = recebida == null ? null : Math.max(0, Duration.between(recebida, agora).toMinutes());
                    return new FilaLojaResponse(
                            rs.getLong("id"),
                            rs.getString("numero_pedido"),
                            rs.getString("produto"),
                            rs.getInt("quantidade"),
                            rs.getString("status"),
                            rs.getString("destino_final"),
                            rs.getObject("ong_destinataria_id", Long.class),
                            recebida,
                            minutos,
                            rs.getString("motivo"),
                            rs.getString("tipo_resolucao")
                    );
                }
        );
        return new PaginaResponse<>(itens, pagina, tamanho, total);
    }

    private AcompanhamentoResponse acompanhamento(ResultSet rs) throws SQLException {
        return new AcompanhamentoResponse(
                rs.getLong("id"),
                rs.getString("numero_pedido"),
                rs.getString("produto"),
                rs.getInt("quantidade"),
                rs.getLong("loja_id"),
                rs.getString("loja"),
                rs.getString("endereco"),
                rs.getObject("data_limite_devolucao", LocalDate.class),
                rs.getString("status"),
                rs.getString("destino_final"),
                rs.getObject("ong_destinataria_id", Long.class),
                hora(rs, "data_criacao"),
                hora(rs, "data_recebimento"),
                hora(rs, "data_definicao_destino"),
                hora(rs, "data_conclusao"),
                hora(rs, "qr_expira_em"),
                rs.getString("motivo"),
                rs.getString("tipo_resolucao"),
                rs.getString("motivo_reprovacao"),
                rs.getString("observacao_conferencia")
        );
    }

    public static Map<String, Object> parametros(int pagina, int tamanho) {
        PaginaResponse.validar(pagina, tamanho);
        var params = new HashMap<String, Object>();
        params.put("limite", tamanho);
        params.put("offset", (long) pagina * tamanho);
        return params;
    }

    public static LocalDateTime hora(ResultSet rs, String campo) throws SQLException {
        return rs.getObject(campo, LocalDateTime.class);
    }
}
