package com.magalu.devolucao.domain;

import com.magalu.devolucao.domain.enums.StatusQRCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qrcodes_devolucao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRCodeDevolucao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Token opaco e nao sequencial: o QR carrega ISSO, nunca o ID da
    // solicitacao. Evita que alguem gere/adivinhe QR de outro usuario.
    @Column(nullable = false, unique = true, updatable = false)
    private UUID token;

    // Base64 da imagem gerada pelo ZXing, para nao precisar regerar
    // a cada exibicao na tela do app.
    @Column(columnDefinition = "text")
    private String imagemBase64;

    @Column(nullable = false)
    private LocalDateTime dataGeracao;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusQRCode status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoDevolucao solicitacao;

    public boolean isExpirado(LocalDateTime agora) {
        return !agora.isBefore(dataExpiracao);
    }

    public boolean isValido(LocalDateTime agora) {
        return status == StatusQRCode.ATIVO && !isExpirado(agora);
    }

    public void cancelar() {
        if (status == StatusQRCode.ATIVO) status = StatusQRCode.CANCELADO;
    }

    public void invalidar(LocalDateTime agora) {
        if (this.status == StatusQRCode.ATIVO) {
            this.status = isExpirado(agora) ? StatusQRCode.EXPIRADO : StatusQRCode.UTILIZADO;
        }
    }
}
