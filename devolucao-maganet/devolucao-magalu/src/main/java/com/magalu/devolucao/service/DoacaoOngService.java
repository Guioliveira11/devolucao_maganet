package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.RegistroAuditoria;
import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.dto.DoacaoOngResponse;
import com.magalu.devolucao.dto.PaginaResponse;
import com.magalu.devolucao.exception.ParametroInvalidoException;
import com.magalu.devolucao.exception.RecursoNaoEncontradoException;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import com.magalu.devolucao.repository.RegistroAuditoriaRepository;
import com.magalu.devolucao.repository.SolicitacaoDevolucaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Doacoes encaminhadas a ONG autenticada. A ONG vem sempre da conta logada,
 * nunca de um id enviado pelo cliente da API.
 */
@Service
@RequiredArgsConstructor
public class DoacaoOngService {

    private final Clock clock;
    private final IdentidadeService identidade;
    private final SolicitacaoDevolucaoRepository solicitacaoRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final NotificacaoService notificacoes;

    @Transactional(readOnly = true)
    public PaginaResponse<DoacaoOngResponse> listar(String status, int pagina, int tamanho) {
        PaginaResponse.validar(pagina, tamanho);
        var ong = identidade.ong();
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());

        Page<SolicitacaoDevolucao> encontradas;
        if (status == null || status.isBlank()) {
            encontradas = solicitacaoRepository.findByOngDestinataria_IdAndDestinoFinal(
                    ong.getId(), DestinoProduto.DOACAO_ONG, pageable);
        } else {
            StatusSolicitacao filtro;
            if ("DESTINO_PENDENTE".equals(status)) filtro = StatusSolicitacao.DESTINO_PENDENTE;
            else if ("CONCLUIDA".equals(status)) filtro = StatusSolicitacao.CONCLUIDA;
            else throw new ParametroInvalidoException("Status inválido para doações.");
            encontradas = solicitacaoRepository.findByOngDestinataria_IdAndDestinoFinalAndStatus(
                    ong.getId(), DestinoProduto.DOACAO_ONG, filtro, pageable);
        }
        var itens = encontradas.getContent().stream().map(DoacaoOngService::paraResposta).toList();
        return new PaginaResponse<>(itens, pagina, tamanho, encontradas.getTotalElements());
    }

    /** A ONG confirma, por decisao humana, que recebeu os produtos. Nada e concluido automaticamente. */
    @Transactional
    public DoacaoOngResponse confirmarRecebimento(Long id) {
        var ong = identidade.ong();
        var solicitacao = solicitacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada"));
        // Doacao de outra ONG responde 404, sem revelar que ela existe.
        if (solicitacao.getOngDestinataria() == null
                || !Objects.equals(solicitacao.getOngDestinataria().getId(), ong.getId())
                || solicitacao.getDestinoFinal() != DestinoProduto.DOACAO_ONG)
            throw new RecursoNaoEncontradoException("Doação não encontrada");

        if (solicitacao.getStatus() == StatusSolicitacao.CONCLUIDA) return paraResposta(solicitacao);
        if (solicitacao.getStatus() != StatusSolicitacao.DESTINO_PENDENTE)
            throw new TransicaoInvalidaException("Esta doação ainda não está disponível para confirmação.");

        var agora = LocalDateTime.now(clock);
        solicitacao.concluir(agora);
        var evento = registroAuditoriaRepository.save(RegistroAuditoria.builder().solicitacao(solicitacao)
                .autor("ONG:" + ong.getId()).acao("DOACAO_RECEBIDA_PELA_ONG").timestamp(agora).build());
        notificacoes.registrarEvento(evento);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return paraResposta(solicitacao);
    }

    private static DoacaoOngResponse paraResposta(SolicitacaoDevolucao s) {
        var item = s.getItemComprado();
        var produto = item.getProduto();
        return new DoacaoOngResponse(s.getId(), item.getPedido().getNumeroPedido(), produto.getNome(),
                produto.getCategoria(), item.getQuantidade(), s.getLojaEscolhida().getNome(),
                s.getStatus().name(), s.getDataDefinicaoDestino(), s.getDataConclusao());
    }
}
