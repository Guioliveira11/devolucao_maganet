package com.magalu.devolucao.repository;

import com.magalu.devolucao.dto.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import static com.magalu.devolucao.repository.PortalRepository.*;

@Repository @RequiredArgsConstructor
public class NotificacaoRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public PreferenciasNotificacao preferencias(long usuarioId){
        return jdbc.query("SELECT atualizacoes,lembretes_qr FROM preferencias_notificacao WHERE usuario_id=:usuario",
                Map.of("usuario",usuarioId),(rs,n)->new PreferenciasNotificacao(rs.getBoolean(1),rs.getBoolean(2)))
                .stream().findFirst().orElse(new PreferenciasNotificacao(true,true));
    }
    public void salvarPreferencias(long usuarioId,PreferenciasNotificacao p){
        jdbc.update("""
            INSERT INTO preferencias_notificacao(usuario_id,atualizacoes,lembretes_qr)
            VALUES (:usuario,:atualizacoes,:lembretes)
            ON CONFLICT (usuario_id) DO UPDATE SET atualizacoes=excluded.atualizacoes,lembretes_qr=excluded.lembretes_qr
            """,Map.of("usuario",usuarioId,"atualizacoes",p.atualizacoes(),"lembretes",p.lembretesQr()));
    }
    public int inserir(long usuarioId,long solicitacaoId,String chave,String tipo,String mensagem,
            LocalDateTime agora,LocalDateTime expiraEm,boolean lembrete){
        var params=new HashMap<String,Object>();params.put("usuario",usuarioId);params.put("solicitacao",solicitacaoId);
        params.put("chave",chave);params.put("tipo",tipo);params.put("mensagem",mensagem);params.put("agora",agora);
        params.put("expira",expiraEm);
        // Coluna escolhida internamente, nunca interpolada a partir da requisicao.
        String preferencia=lembrete?"lembretes_qr":"atualizacoes";
        return jdbc.update("""
            INSERT INTO notificacoes(usuario_id,solicitacao_id,chave,tipo,mensagem,criada_em,qr_expira_em)
            SELECT :usuario,:solicitacao,:chave,:tipo,:mensagem,:agora,:expira
            WHERE coalesce((SELECT %s FROM preferencias_notificacao WHERE usuario_id=:usuario),true)
            ON CONFLICT (chave) DO NOTHING
            """.formatted(preferencia),params);
    }
    public PaginaResponse<NotificacaoResponse> listar(long usuarioId,boolean naoLidas,int pagina,int tamanho){
        var params=parametros(pagina,tamanho);params.put("usuario",usuarioId);
        String where=" WHERE usuario_id=:usuario"+(naoLidas?" AND lida_em IS NULL":"");
        long total=jdbc.queryForObject("SELECT count(*) FROM notificacoes"+where,params,Long.class);
        var itens=jdbc.query("SELECT id,solicitacao_id,tipo,mensagem,criada_em,lida_em,qr_expira_em FROM notificacoes"+where+
                " ORDER BY criada_em DESC,id DESC LIMIT :limite OFFSET :offset",params,
                (rs,n)->new NotificacaoResponse(rs.getLong("id"),rs.getLong("solicitacao_id"),rs.getString("tipo"),
                    rs.getString("mensagem"),hora(rs,"criada_em"),hora(rs,"lida_em"),hora(rs,"qr_expira_em")));
        return new PaginaResponse<>(itens,pagina,tamanho,total);
    }
    public int marcarLida(long usuarioId,long id,LocalDateTime agora){
        return jdbc.update("UPDATE notificacoes SET lida_em=coalesce(lida_em,:agora) WHERE id=:id AND usuario_id=:usuario",
                Map.of("id",id,"usuario",usuarioId,"agora",agora));
    }

    public record Lembrete(long qrId,long solicitacaoId,long usuarioId,LocalDateTime expiraEm) {}
    public List<Lembrete> bloquearLembretes(LocalDateTime agora,LocalDateTime limite,int tamanhoLote){
        // Mesma ordem de bloqueio do escaneamento/cancelamento: solicitacao primeiro.
        // SKIP LOCKED permite varios workers; a chave unica protege contra repeticao.
        return jdbc.query("""
            SELECT q.id AS qr_id,s.id AS solicitacao_id,s.usuario_id,q.data_expiracao
            FROM solicitacoes_devolucao s JOIN qrcodes_devolucao q ON q.solicitacao_id=s.id
            LEFT JOIN preferencias_notificacao p ON p.usuario_id=s.usuario_id
            WHERE s.status='AGUARDANDO_ESCANEAMENTO' AND q.status='ATIVO'
              AND q.data_expiracao > :agora AND q.data_expiracao <= :limite
              AND coalesce(p.lembretes_qr,true)
              AND NOT EXISTS (SELECT 1 FROM notificacoes n WHERE n.chave='qr:' || q.id)
            ORDER BY q.data_expiracao,q.id LIMIT :tamanhoLote FOR UPDATE OF s SKIP LOCKED
            """,Map.of("agora",agora,"limite",limite,"tamanhoLote",tamanhoLote),
                (rs,n)->new Lembrete(rs.getLong("qr_id"),rs.getLong("solicitacao_id"),rs.getLong("usuario_id"),hora(rs,"data_expiracao")));
    }
    public record Pendencias(long quantidade, LocalDateTime proximoVencimento) {}
    public Pendencias pendencias(LocalDateTime agora, LocalDateTime limite) {
        return jdbc.queryForObject("""
            SELECT count(*) AS quantidade, min(q.data_expiracao) AS proximo
            FROM solicitacoes_devolucao s JOIN qrcodes_devolucao q ON q.solicitacao_id=s.id
            LEFT JOIN preferencias_notificacao p ON p.usuario_id=s.usuario_id
            WHERE s.status='AGUARDANDO_ESCANEAMENTO' AND q.status='ATIVO'
              AND q.data_expiracao > :agora AND q.data_expiracao <= :limite
              AND coalesce(p.lembretes_qr,true)
              AND NOT EXISTS (SELECT 1 FROM notificacoes n WHERE n.chave='qr:' || q.id)
            """, Map.of("agora",agora,"limite",limite),
                (rs,n) -> new Pendencias(rs.getLong("quantidade"),hora(rs,"proximo")));
    }

    public boolean lembreteAindaValido(Lembrete l,LocalDateTime agora){
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM solicitacoes_devolucao s JOIN qrcodes_devolucao q ON q.solicitacao_id=s.id
            WHERE s.id=:solicitacao AND q.id=:qr AND s.status='AGUARDANDO_ESCANEAMENTO'
              AND q.status='ATIVO' AND q.data_expiracao > :agora)
            """,Map.of("solicitacao",l.solicitacaoId(),"qr",l.qrId(),"agora",agora),Boolean.class));
    }
}
