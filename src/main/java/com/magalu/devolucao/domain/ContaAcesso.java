package com.magalu.devolucao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "contas_acesso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String senhaHash;

    @Builder.Default
    private boolean ativa = true;

    @Builder.Default
    @Column(name = "tentativas_falhas")
    private int tentativasFalhas = 0;
    
    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", unique = true)
    private Funcionario funcionario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_id", unique = true)
    private OngParceira ong;

    public void registrarFalha(LocalDateTime agora, int maxTentativas, int minutosBloqueio) {
        this.tentativasFalhas++;
        if (this.tentativasFalhas >= maxTentativas) {
            this.bloqueadoAte = agora.plusMinutes(minutosBloqueio);
        }
    }

    public void registrarSucesso() {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public boolean isBloqueada(LocalDateTime agora) {
        return this.bloqueadoAte != null && this.bloqueadoAte.isAfter(agora);
    }
}
