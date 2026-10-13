package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.exception.*;
import com.magalu.devolucao.repository.*;
import com.magalu.devolucao.service.strategy.DestinoStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class DevolucaoService {
    private final java.time.Clock clock;
    private final NotificacaoService notificacoes;
    private final SolicitacaoDevolucaoRepository solicitacaoRepository;
    private final ItemCompradoRepository itemCompradoRepository;
    private final LojaRepository lojaRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final IdentidadeService identidade;
    private final QRCodeService qrCodeService;
    private final DestinoStrategyResolver destinoStrategyResolver;

    @Transactional
    public SolicitacaoResponse criarSolicitacao(Long itemCompradoId, Long lojaId) {
        Usuario usuario = identidade.cliente();
        // Serializa criacoes para o mesmo item; o indice parcial e a defesa no banco.
        ItemComprado item = itemCompradoRepository.findByIdForUpdate(itemCompradoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item comprado nao encontrado"));
        validarDono(item.getPedido().getUsuario(), usuario);
        if (!item.dentroDoPrazoDevolucao(java.time.LocalDate.now(clock))) throw new com.magalu.devolucao.exception.RegraNegocioException("Item fora do prazo de devolucao");
        if (solicitacaoRepository.existsByItemCompradoIdAndStatusNot(item.getId(), StatusSolicitacao.CANCELADA))
            throw new com.magalu.devolucao.exception.RegraNegocioException("Item ja possui devolucao aberta ou concluida");
        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja nao encontrada"));
        validarLojaAtiva(loja);
        var solicitacao = SolicitacaoDevolucao.builder().usuario(usuario).itemComprado(item)
                .lojaEscolhida(loja).status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO)
                .dataCriacao(LocalDateTime.now(clock)).build();
        solicitacaoRepository.save(solicitacao);
        qrCodeService.gerarQRCode(solicitacao);
        registrarAuditoria(solicitacao, "CLIENTE:" + usuario.getId(), "SOLICITACAO_CRIADA", null);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse buscarPorId(Long id) {
        var usuario = identidade.cliente();
        var solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
        validarDono(solicitacao.getUsuario(), usuario);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ItemCompradoResponse> listarItens(int pagina, int tamanho) {
        PaginaResponse.validar(pagina, tamanho);
        var usuario = identidade.cliente();
        var pageable = org.springframework.data.domain.PageRequest.of(pagina, tamanho,
                org.springframework.data.domain.Sort.by("id").descending());
        var encontrados = itemCompradoRepository.findByPedido_Usuario_Id(usuario.getId(), pageable);
        var ids = encontrados.getContent().stream().map(ItemComprado::getId).toList();
        Set<Long> bloqueados = ids.isEmpty() ? Set.of() :
                solicitacaoRepository.findItensBloqueados(usuario.getId(), ids);
        var hoje = java.time.LocalDate.now(clock);
        var itens = encontrados.getContent().stream()
                .map(item -> new ItemCompradoResponse(item.getId(), item.getPedido().getNumeroPedido(),
                        item.getProduto().getNome(), item.getQuantidade(), item.getValorPago(),
                        item.getDataLimiteDevolucao(), item.dentroDoPrazoDevolucao(hoje) &&
                        !bloqueados.contains(item.getId())))
                .toList();
        return new PaginaResponse<>(itens, pagina, tamanho, encontrados.getTotalElements());
    }

    @Transactional
    public SolicitacaoResponse renovarQRCode(Long id) {
        var usuario = identidade.cliente();
        var solicitacao = bloquear(id);
        validarDono(solicitacao.getUsuario(), usuario);
        validarAntesDoRecebimento(solicitacao);
        validarLojaAtiva(solicitacao.getLojaEscolhida());
        if (!solicitacao.getItemComprado().dentroDoPrazoDevolucao(java.time.LocalDate.now(clock)))
            throw new com.magalu.devolucao.exception.RegraNegocioException("Item fora do prazo de devolucao");
        // Repetir o pedido com QR valido e idempotente; nao troca o token.
        if (solicitacao.qrCodeAtivo(LocalDateTime.now(clock)) == null) {
            qrCodeService.gerarQRCode(solicitacao);
            registrarAuditoria(solicitacao, "CLIENTE:" + usuario.getId(), "QR_RENOVADO", null);
        }
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional
    public SolicitacaoResponse cancelar(Long id) {
        var usuario = identidade.cliente();
        var solicitacao = bloquear(id);
        validarDono(solicitacao.getUsuario(), usuario);
        if (solicitacao.getStatus() != StatusSolicitacao.CANCELADA) {
            validarAntesDoRecebimento(solicitacao);
            solicitacao.transicionarEstado(StatusSolicitacao.CANCELADA);
            solicitacao.getHistoricoQrCodes().forEach(QRCodeDevolucao::cancelar);
            registrarAuditoria(solicitacao, "CLIENTE:" + usuario.getId(), "SOLICITACAO_CANCELADA", null);
            solicitacaoRepository.saveAndFlush(solicitacao);
        }
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional
    public SolicitacaoResponse processarEscaneamento(UUID token) {
        var funcionario = identidade.funcionario();
        var solicitacao = qrCodeService.validarQRCode(token);
        validarFuncionarioNaLoja(funcionario, solicitacao);
        var agora = LocalDateTime.now(clock);
        var qr = solicitacao.qrCodeAtivo(agora);
        if (qr == null) throw new QRCodeInvalidoException("QR Code expirado");
        solicitacao.confirmarEscaneamento(funcionario, agora);
        qr.invalidar(agora);
        registrarAuditoria(solicitacao, funcionario.getMatricula(), "QR_ESCANEADO", null);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional
    public SolicitacaoResponse definirDestinoFinal(Long id, DestinoProduto destino) {
        var funcionario = identidade.funcionario();
        var solicitacao = bloquear(id);
        validarFuncionarioNaLoja(funcionario, solicitacao);
        if (solicitacao.getStatus() != StatusSolicitacao.EM_ANALISE)
            throw new TransicaoInvalidaException("Somente solicitacoes em analise podem receber destino");
        destinoStrategyResolver.resolver(destino).processar(solicitacao);
        solicitacao.definirDestino(destino, LocalDateTime.now(clock));
        registrarAuditoria(solicitacao, funcionario.getMatricula(), "DESTINO_DEFINIDO", "destino=" + destino);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    private SolicitacaoDevolucao bloquear(Long id) {
        return solicitacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    }
    private void validarDono(Usuario dono, Usuario atual) {
        if (!Objects.equals(dono.getId(), atual.getId())) throw new AccessDeniedException("Item ou solicitacao de outro cliente");
    }
    private void validarLojaAtiva(Loja loja) {
        if (!loja.isAtiva()) throw new com.magalu.devolucao.exception.RegraNegocioException("Loja inativa");
    }
    private void validarFuncionarioNaLoja(Funcionario funcionario, SolicitacaoDevolucao solicitacao) {
        if (!Objects.equals(funcionario.getLoja().getId(), solicitacao.getLojaEscolhida().getId()))
            throw new AccessDeniedException("Funcionario nao pertence a loja da solicitacao");
        validarLojaAtiva(solicitacao.getLojaEscolhida());
    }
    private void validarAntesDoRecebimento(SolicitacaoDevolucao solicitacao) {
        if (solicitacao.getStatus() != StatusSolicitacao.AGUARDANDO_ESCANEAMENTO &&
                solicitacao.getStatus() != StatusSolicitacao.QR_EXPIRADO)
            throw new TransicaoInvalidaException("Acao permitida somente antes do recebimento na loja");
    }
    private void registrarAuditoria(SolicitacaoDevolucao s, String autor, String acao, String detalhes) {
        var evento = registroAuditoriaRepository.save(RegistroAuditoria.builder().solicitacao(s).autor(autor)
                .acao(acao).timestamp(LocalDateTime.now(clock)).detalhes(detalhes).build());
        notificacoes.registrarEvento(evento);
    }
}
