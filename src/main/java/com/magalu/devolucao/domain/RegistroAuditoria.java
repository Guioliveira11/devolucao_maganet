package com.magalu.devolucao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoDevolucao solicitacao;

    // Guardado como String (nao FK para Usuario/Funcionario) de proposito:
    // o registro de auditoria deve sobreviver mesmo que o autor original
    // seja removido do sistema no futuro (LGPD / retencao de dados).
    private String autor;

    private String acao;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "text")
    private String detalhes;
}
