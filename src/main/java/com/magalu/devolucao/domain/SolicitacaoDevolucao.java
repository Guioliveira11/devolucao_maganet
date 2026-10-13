package com.magalu.devolucao.domain;

import com.magalu.devolucao.domain.enums.DestinoProduto;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import com.magalu.devolucao.exception.TransicaoInvalidaException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "solicitacoes_devolucao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoDevolucao {

    // Transicoes permitidas. Mantido como regra estatica simples em vez de
    // classes de estado separadas (GoF State completo) porque o numero de
    // estados e pequeno; se o fluxo crescer, migrar para uma classe por estado.
    private static final Map<StatusSolicitacao, EnumSet<StatusSolicitacao>> TRANSICOES_PERMITIDAS =
            new EnumMap<>(StatusSolicitacao.class);

    static {
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO,
                EnumSet.of(StatusSolicitacao.EM_ANALISE, StatusSolicitacao.QR_EXPIRADO, StatusSolicitacao.CANCELADA));
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.QR_EXPIRADO,
                EnumSet.of(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO, StatusSolicitacao.CANCELADA));
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.EM_ANALISE,
                EnumSet.of(StatusSolicitacao.DESTINO_PENDENTE, StatusSolicitacao.CANCELADA));
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.DESTINO_PENDENTE, EnumSet.of(StatusSolicitacao.CONCLUIDA));
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.CONCLUIDA, EnumSet.noneOf(StatusSolicitacao.class));
        TRANSICOES_PERMITIDAS.put(StatusSolicitacao.CANCELADA, EnumSet.noneOf(StatusSolicitacao.class));
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_comprado_id")
    private ItemComprado itemComprado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id")
    private Loja lojaEscolhida;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL)
    @Builder.Default
    private List<QRCodeDevolucao> historicoQrCodes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;

    @Enumerated(EnumType.STRING)
    private DestinoProduto destinoFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_destinataria_id")
    private OngParceira ongDestinataria;

    private LocalDateTime dataDefinicaoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_responsavel_id")
    private Funcionario funcionarioResponsavel;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataConclusao;

    private LocalDateTime dataRecebimento;

    // Lock otimista: evita que dois funcionarios processem o mesmo
    // escaneamento simultaneamente (race condition no balcao da loja).
    @Version
    private Long version;

    /**
     * QR Code atualmente valido, se existir. Usado pelo QRCodeService
     * para decidir se gera um novo ou reaproveita o existente.
     */
    public QRCodeDevolucao qrCodeAtivo(LocalDateTime agora) {
        return historicoQrCodes.stream()
                .filter(qr -> qr.isValido(agora))
                .findFirst()
                .orElse(null);
    }

    public void adicionarQrCode(QRCodeDevolucao novoQr) {
        this.historicoQrCodes.add(novoQr);
        novoQr.setSolicitacao(this);
    }

    public void confirmarEscaneamento(Funcionario funcionario, LocalDateTime agora) {
        transicionarEstado(StatusSolicitacao.EM_ANALISE);
        this.funcionarioResponsavel = funcionario;
        this.dataRecebimento = agora;
    }

    public void definirDestino(DestinoProduto destino, LocalDateTime agora) {
        if (destino == null || (destino == DestinoProduto.DOACAO_ONG && ongDestinataria == null))
            throw new TransicaoInvalidaException("Destino ou ONG destinataria nao definido");
        transicionarEstado(StatusSolicitacao.DESTINO_PENDENTE);
        this.destinoFinal = destino;
        this.dataDefinicaoDestino = agora;
    }

    // Reservado para futura confirmacao confiavel da integracao; sem endpoint publico.
    public void concluir(LocalDateTime agora) {
        transicionarEstado(StatusSolicitacao.CONCLUIDA);
        this.dataConclusao = agora;
    }

    /**
     * Ponto unico de mudanca de status: garante que nenhuma transicao
     * fora do fluxo definido aconteca (ex: pular direto de
     * AGUARDANDO_ESCANEAMENTO para CONCLUIDA).
     */
    public void transicionarEstado(StatusSolicitacao novoStatus) {
        EnumSet<StatusSolicitacao> permitidas = TRANSICOES_PERMITIDAS.get(this.status);
        if (permitidas == null || !permitidas.contains(novoStatus)) {
            throw new TransicaoInvalidaException(
                    "Transicao de " + this.status + " para " + novoStatus + " nao e permitida");
        }
        this.status = novoStatus;
    }
}
