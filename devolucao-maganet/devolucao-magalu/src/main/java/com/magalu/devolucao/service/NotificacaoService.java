package com.magalu.devolucao.service;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.domain.RegistroAuditoria;
import com.magalu.devolucao.domain.enums.EventoCliente;
import com.magalu.devolucao.exception.RecursoNaoEncontradoException;
import com.magalu.devolucao.repository.NotificacaoRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class NotificacaoService {
    private final NotificacaoRepository repo;
    private final IdentidadeService identidade;
    private final Clock clock;

    @Transactional(propagation=Propagation.MANDATORY)
    public void registrarEvento(RegistroAuditoria evento){
        EventoCliente tipo;
        try { tipo=EventoCliente.valueOf(evento.getAcao()); }
        catch(IllegalArgumentException e){return;}
        var s=evento.getSolicitacao();
        String mensagem = tipo.mensagem();
        if (tipo == EventoCliente.DESTINO_DEFINIDO &&
                s.getStatus() == com.magalu.devolucao.domain.enums.StatusSolicitacao.DESTINO_PENDENTE)
            mensagem = "O destino do produto foi definido e aguarda processamento.";
        repo.inserir(s.getUsuario().getId(),s.getId(),"evento:"+evento.getId(),tipo.name(),mensagem,
                evento.getTimestamp(),null,false);
    }
    @Transactional(readOnly=true)
    public PaginaResponse<NotificacaoResponse> listar(boolean naoLidas,int pagina,int tamanho){
        PaginaResponse.validar(pagina,tamanho);
        return repo.listar(identidade.cliente().getId(),naoLidas,pagina,tamanho);
    }
    @Transactional
    public void marcarLida(long id){
        if(repo.marcarLida(identidade.cliente().getId(),id,LocalDateTime.now(clock))==0)
            throw new RecursoNaoEncontradoException("Notificacao nao encontrada");
    }
    @Transactional(readOnly=true)
    public PreferenciasNotificacao preferencias(){return repo.preferencias(identidade.cliente().getId());}
    @Transactional
    public PreferenciasNotificacao salvarPreferencias(PreferenciasNotificacao p){
        if(p.atualizacoes()==null || p.lembretesQr()==null) throw new com.magalu.devolucao.exception.ParametroInvalidoException("Informe as duas preferencias");
        repo.salvarPreferencias(identidade.cliente().getId(),p);return p;
    }
    @Transactional(readOnly=true)
    public NotificacaoRepository.Pendencias pendenciasLembretes(int antecedenciaMinutos) {
        var agora = LocalDateTime.now(clock);
        return repo.pendencias(agora, agora.plusMinutes(antecedenciaMinutos));
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public ResultadoLote gerarLembretes(int antecedenciaMinutos, int tamanhoLote){
        ResultadoLote.validarTamanho(tamanhoLote);
        if(antecedenciaMinutos<1 || antecedenciaMinutos>1440)
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Antecedencia deve estar entre 1 e 1440 minutos");
        var agora=LocalDateTime.now(clock);
        var candidatos=repo.bloquearLembretes(agora,agora.plusMinutes(antecedenciaMinutos),tamanhoLote);
        int processados=0;
        for(var l:candidatos){
            var momento=LocalDateTime.now(clock);
            if(!repo.lembreteAindaValido(l,momento))continue;
            processados += repo.inserir(l.usuarioId(),l.solicitacaoId(),"qr:"+l.qrId(),"QR_PRESTES_A_EXPIRAR",
                    "O QR da sua devolucao esta perto de expirar. Consulte a solicitacao para ver o prazo.",
                    momento,l.expiraEm(),true);
        }
        return new ResultadoLote(candidatos.size(), processados);
    }
}
