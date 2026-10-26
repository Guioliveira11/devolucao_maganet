package com.magalu.devolucao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Foto enviada pelo cliente para mostrar o estado do produto.
 * Guardada em base64 (sem o prefixo "data:") junto com o tipo da imagem.
 */
@Entity
@Table(name = "evidencias_devolucao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaDevolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoDevolucao solicitacao;

    @Column(name = "tipo_conteudo", nullable = false)
    private String tipoConteudo;

    @Column(name = "conteudo_base64", nullable = false, columnDefinition = "text")
    private String conteudoBase64;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    public String comoDataUrl() {
        return "data:" + tipoConteudo + ";base64," + conteudoBase64;
    }
}
