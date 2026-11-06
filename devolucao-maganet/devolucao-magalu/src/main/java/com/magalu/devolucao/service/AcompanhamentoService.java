package com.magalu.devolucao.service;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.repository.PortalRepository;
import com.magalu.devolucao.exception.RecursoNaoEncontradoException;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AcompanhamentoService {
    private final PortalRepository portal;
    private final IdentidadeService identidade;
    private final Clock clock;
    public PaginaResponse<AcompanhamentoResponse> listar(StatusSolicitacao status,int pagina,int tamanho){
        PaginaResponse.validar(pagina,tamanho);
        return portal.listarCliente(identidade.cliente().getId(),status,pagina,tamanho,LocalDateTime.now(clock));
    }
    public AcompanhamentoResponse detalhe(long id){
        return portal.detalheCliente(identidade.cliente().getId(),id,LocalDateTime.now(clock))
                .orElseThrow(()->new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    }
    public PaginaResponse<EventoClienteResponse> historico(long id,int pagina,int tamanho){
        PaginaResponse.validar(pagina,tamanho);
        var usuario=identidade.cliente();
        portal.detalheCliente(usuario.getId(),id,LocalDateTime.now(clock))
                .orElseThrow(()->new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
        return portal.historico(usuario.getId(),id,pagina,tamanho);
    }
    public PaginaResponse<FilaLojaResponse> fila(StatusSolicitacao status,DestinoProduto destino,Long minimoHoras,int pagina,int tamanho){
        PaginaResponse.validar(pagina,tamanho);
        if(status!=null && status!=StatusSolicitacao.EM_ANALISE && status!=StatusSolicitacao.DESTINO_PENDENTE)
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Fila aceita somente EM_ANALISE ou DESTINO_PENDENTE");
        if(minimoHoras!=null && (minimoHoras<0 || minimoHoras>876000))
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Tempo minimo deve estar entre 0 e 876000 horas");
        var loja=identidade.funcionario().getLoja();
        if(!loja.isAtiva()) throw new com.magalu.devolucao.exception.RegraNegocioException("Loja inativa");
        return portal.fila(loja.getId(),status,destino,minimoHoras,pagina,tamanho,LocalDateTime.now(clock));
    }
}
