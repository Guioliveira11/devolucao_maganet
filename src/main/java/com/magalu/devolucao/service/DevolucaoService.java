package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.EvidenciaDevolucao;
import com.magalu.devolucao.domain.Funcionario;
import com.magalu.devolucao.domain.ItemComprado;
import com.magalu.devolucao.domain.Loja;
import com.magalu.devolucao.domain.QRCodeDevolucao;
import com.magalu.devolucao.domain.RegistroAuditoria;
import com.magalu.devolucao.domain.SolicitacaoDevolucao;
import com.magalu.devolucao.domain.Usuario;
import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.ItemConferencia;
import com.magalu.devolucao.domain.enums.MotivoDevolucao;
import com.magalu.devolucao.domain.enums.MotivoReprovacao;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.domain.enums.TipoResolucao;
import com.magalu.devolucao.dto.ConferenciaResponse;
import com.magalu.devolucao.dto.CriarSolicitacaoRequest;
import com.magalu.devolucao.dto.DefinirDestinoRequest;
import com.magalu.devolucao.dto.ItemCompradoResponse;
import com.magalu.devolucao.dto.PaginaResponse;
import com.magalu.devolucao.dto.ReprovarDevolucaoRequest;
import com.magalu.devolucao.dto.SolicitacaoResponse;
import com.magalu.devolucao.exception.ParametroInvalidoException;
import com.magalu.devolucao.exception.QRCodeInvalidoException;
import com.magalu.devolucao.exception.RecursoNaoEncontradoException;
import com.magalu.devolucao.exception.RegraNegocioException;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import com.magalu.devolucao.repository.ItemCompradoRepository;
import com.magalu.devolucao.repository.LojaRepository;
import com.magalu.devolucao.repository.RegistroAuditoriaRepository;
import com.magalu.devolucao.repository.SolicitacaoDevolucaoRepository;
import com.magalu.devolucao.service.strategy.DestinoStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevolucaoService {

    private static final int MAX_EVIDENCIAS = 3;
    private static final int MAX_BASE64_CHARS = 700_000;
    private static final Set<String> TIPOS_IMAGEM = Set.of("image/jpeg", "image/png", "image/webp");

    private final Clock clock;
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
        return criarSolicitacao(itemCompradoId, lojaId, null, null, null, List.of());
    }

    @Transactional
    public SolicitacaoResponse criarSolicitacao(CriarSolicitacaoRequest req) {
        return criarSolicitacao(
                req.itemCompradoId(),
                req.lojaId(),
                req.motivo(),
                req.comentario(),
                req.tipoResolucao(),
                req.evidencias()
        );
    }

    @Transactional
    public SolicitacaoResponse criarSolicitacao(
            Long itemCompradoId,
            Long lojaId,
            MotivoDevolucao motivo,
            String comentario,
            TipoResolucao tipoResolucao,
            List<String> evidenciasDataUrl) {
        Usuario usuario = identidade.cliente();
        // Serializa criacoes para o mesmo item; o indice parcial e a defesa no banco.
        ItemComprado item = itemCompradoRepository.findByIdForUpdate(itemCompradoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item comprado nao encontrado"));
        validarDono(item.getPedido().getUsuario(), usuario);

        if (!item.dentroDoPrazoDevolucao(LocalDate.now(clock))) {
            throw new RegraNegocioException("Item fora do prazo de devolucao");
        }
        if (solicitacaoRepository.existsByItemCompradoIdAndStatusNot(item.getId(), StatusSolicitacao.CANCELADA)) {
            throw new RegraNegocioException("Item ja possui devolucao aberta ou concluida");
        }

        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja nao encontrada"));
        validarLojaAtiva(loja);

        var agora = LocalDateTime.now(clock);
        var solicitacao = SolicitacaoDevolucao.builder()
                .usuario(usuario)
                .itemComprado(item)
                .lojaEscolhida(loja)
                .status(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO)
                .motivo(motivo)
                .comentario(comentario == null || comentario.isBlank() ? null : comentario.strip())
                .tipoResolucao(tipoResolucao)
                .dataCriacao(agora)
                .build();

        anexarEvidencias(solicitacao, evidenciasDataUrl, agora);

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
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());
        var encontrados = itemCompradoRepository.findByPedido_Usuario_Id(usuario.getId(), pageable);
        var ids = encontrados.getContent().stream().map(ItemComprado::getId).toList();
        Set<Long> bloqueados = ids.isEmpty() ? Set.of() :
                solicitacaoRepository.findItensBloqueados(usuario.getId(), ids);
        var hoje = LocalDate.now(clock);
        var itens = encontrados.getContent().stream()
                .map(item -> new ItemCompradoResponse(
                        item.getId(),
                        item.getPedido().getNumeroPedido(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getValorPago(),
                        item.getDataLimiteDevolucao(),
                        item.dentroDoPrazoDevolucao(hoje) && !bloqueados.contains(item.getId())
                ))
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
        if (!solicitacao.getItemComprado().dentroDoPrazoDevolucao(LocalDate.now(clock))) {
            throw new RegraNegocioException("Item fora do prazo de devolucao");
        }
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
        if (qr == null) {
            throw new QRCodeInvalidoException("QR Code expirado");
        }
        solicitacao.confirmarEscaneamento(funcionario, agora);
        qr.invalidar(agora);
        registrarAuditoria(solicitacao, funcionario.getMatricula(), "QR_ESCANEADO", null);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public ConferenciaResponse buscarParaConferencia(Long id) {
        var funcionario = identidade.funcionario();
        var solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
        validarFuncionarioNaLoja(funcionario, solicitacao);
        return ConferenciaResponse.from(solicitacao);
    }

    @Transactional
    public SolicitacaoResponse definirDestinoFinal(Long id, DestinoProduto destino) {
        return definirDestinoFinal(id, destino, List.of());
    }

    @Transactional
    public SolicitacaoResponse definirDestinoFinal(Long id, DefinirDestinoRequest req) {
        return definirDestinoFinal(id, req.destino(), req.itensConferidos());
    }

    @Transactional
    public SolicitacaoResponse definirDestinoFinal(
            Long id, DestinoProduto destino, Collection<ItemConferencia> itensConferidos) {
        var funcionario = identidade.funcionario();
        var solicitacao = bloquear(id);
        validarFuncionarioNaLoja(funcionario, solicitacao);
        if (solicitacao.getStatus() != StatusSolicitacao.EM_ANALISE) {
            throw new TransicaoInvalidaException("Somente solicitacoes em analise podem receber destino");
        }
        var agora = LocalDateTime.now(clock);
        destinoStrategyResolver.resolver(destino).processar(solicitacao);
        solicitacao.registrarConferencia(itensConferidos, agora);
        solicitacao.definirDestino(destino, agora);
        registrarAuditoria(solicitacao, funcionario.getMatricula(), "DESTINO_DEFINIDO", "destino=" + destino);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    @Transactional
    public SolicitacaoResponse reprovarDevolucao(Long id, ReprovarDevolucaoRequest req) {
        return reprovarDevolucao(id, req.motivo(), req.observacao(), req.itensConferidos());
    }

    @Transactional
    public SolicitacaoResponse reprovarDevolucao(
            Long id,
            MotivoReprovacao motivo,
            String observacao,
            Collection<ItemConferencia> itensConferidos) {
        var funcionario = identidade.funcionario();
        var solicitacao = bloquear(id);
        validarFuncionarioNaLoja(funcionario, solicitacao);
        if (solicitacao.getStatus() != StatusSolicitacao.EM_ANALISE) {
            throw new TransicaoInvalidaException("Somente solicitacoes em analise podem ser reprovadas");
        }
        var agora = LocalDateTime.now(clock);
        solicitacao.registrarConferencia(itensConferidos, agora);
        solicitacao.reprovar(motivo, observacao, agora);
        registrarAuditoria(solicitacao, funcionario.getMatricula(), "CONFERENCIA_REPROVADA", "motivo=" + motivo);
        solicitacaoRepository.saveAndFlush(solicitacao);
        return SolicitacaoResponse.from(solicitacao, LocalDateTime.now(clock));
    }

    private void anexarEvidencias(SolicitacaoDevolucao solicitacao, List<String> dataUrls, LocalDateTime agora) {
        if (dataUrls == null || dataUrls.isEmpty()) {
            return;
        }
        if (dataUrls.size() > MAX_EVIDENCIAS) {
            throw new ParametroInvalidoException("Envie no maximo " + MAX_EVIDENCIAS + " fotos");
        }
        for (String dataUrl : dataUrls) {
            solicitacao.adicionarEvidencia(converterEvidencia(dataUrl, agora));
        }
    }

    private EvidenciaDevolucao converterEvidencia(String dataUrl, LocalDateTime agora) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            throw new ParametroInvalidoException("Formato de imagem invalido");
        }
        int separador = dataUrl.indexOf(";base64,");
        if (separador <= 5) {
            throw new ParametroInvalidoException("Formato de imagem invalido");
        }
        String tipo = dataUrl.substring(5, separador).toLowerCase();
        String base64 = dataUrl.substring(separador + 8);
        if (!TIPOS_IMAGEM.contains(tipo)) {
            throw new ParametroInvalidoException("Envie imagens nos formatos JPEG, PNG ou WebP");
        }
        if (base64.isEmpty() || base64.length() > MAX_BASE64_CHARS) {
            throw new ParametroInvalidoException("Imagem excede o tamanho permitido");
        }
        try {
            Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ParametroInvalidoException("Imagem em base64 invalida");
        }
        return EvidenciaDevolucao.builder()
                .tipoConteudo(tipo)
                .conteudoBase64(base64)
                .dataEnvio(agora)
                .build();
    }

    private SolicitacaoDevolucao bloquear(Long id) {
        return solicitacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));
    }

    private void validarDono(Usuario dono, Usuario atual) {
        if (!Objects.equals(dono.getId(), atual.getId())) {
            throw new AccessDeniedException("Item ou solicitacao de outro cliente");
        }
    }

    private void validarLojaAtiva(Loja loja) {
        if (!loja.isAtiva()) {
            throw new RegraNegocioException("Loja inativa");
        }
    }

    private void validarFuncionarioNaLoja(Funcionario funcionario, SolicitacaoDevolucao solicitacao) {
        if (!Objects.equals(funcionario.getLoja().getId(), solicitacao.getLojaEscolhida().getId())) {
            throw new AccessDeniedException("Funcionario nao pertence a loja da solicitacao");
        }
        validarLojaAtiva(solicitacao.getLojaEscolhida());
    }

    private void validarAntesDoRecebimento(SolicitacaoDevolucao solicitacao) {
        if (solicitacao.getStatus() != StatusSolicitacao.AGUARDANDO_ESCANEAMENTO
                && solicitacao.getStatus() != StatusSolicitacao.QR_EXPIRADO) {
            throw new TransicaoInvalidaException("Acao permitida somente antes do recebimento na loja");
        }
    }

    private void registrarAuditoria(SolicitacaoDevolucao s, String autor, String acao, String detalhes) {
        var evento = registroAuditoriaRepository.save(RegistroAuditoria.builder()
                .solicitacao(s)
                .autor(autor)
                .acao(acao)
                .timestamp(LocalDateTime.now(clock))
                .detalhes(detalhes)
                .build());
        notificacoes.registrarEvento(evento);
    }
}
