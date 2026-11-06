package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.TokenRecuperacaoSenha;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import com.magalu.devolucao.repository.TokenRecuperacaoSenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {
    private final ContaAcessoRepository contas;
    private final TokenRecuperacaoSenhaRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Clock clock;

    @Value("${app.recuperacao-senha.url-publica:http://localhost:8080}")
    private String urlPublica;

    @Value("${spring.mail.username:}")
    private String remetente;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void solicitar(String email) {
        contas.findAtivaByEmail(email.trim()).ifPresent(conta -> {
            // Um novo pedido invalida qualquer link anterior para a mesma conta.
            tokens.deleteByContaId(conta.getId());

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha();
            registro.setConta(conta);
            registro.setTokenHash(sha256(token));
            registro.setExpiraEm(LocalDateTime.now(clock).plusMinutes(15));
            tokens.save(registro);

            String link = urlPublica.replaceAll("/$", "") + "/?token=" + token;
            SimpleMailMessage mensagem = new SimpleMailMessage();
            if (remetente != null && !remetente.isBlank()) mensagem.setFrom(remetente);
            mensagem.setTo(email.trim());
            mensagem.setSubject("Maga Next - recuperação de senha");
            mensagem.setText("Recebemos uma solicitação para redefinir sua senha no Maga Next.\n\n"
                    + "Acesse o link abaixo para cadastrar uma nova senha (válido por 15 minutos e utilizável uma única vez):\n"
                    + link + "\n\nSe você não solicitou essa alteração, ignore este e-mail.");
            mailSender.send(mensagem);
        });
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        if (token == null || token.isBlank()) return false;
        return tokens.findByTokenHash(sha256(token))
                .filter(t -> t.getUtilizadoEm() == null)
                .filter(t -> t.getExpiraEm().isAfter(LocalDateTime.now(clock)))
                .isPresent();
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRecuperacaoSenha registro = tokens.findByTokenHashForUpdate(sha256(token))
                .filter(t -> t.getUtilizadoEm() == null)
                .filter(t -> t.getExpiraEm().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou expirado. Solicite uma nova recuperação."));

        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter no máximo 72 bytes em UTF-8.");
        }
        var conta = registro.getConta();
        conta.setSenhaHash(passwordEncoder.encode(novaSenha));
        conta.registrarSucesso();
        registro.setUtilizadoEm(LocalDateTime.now(clock));
        // Remove outros tokens eventualmente emitidos, preservando o token utilizado.
        tokens.deleteByContaIdAndIdNot(conta.getId(), registro.getId());
    }

    private String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível processar o token de recuperação.", e);
        }
    }
}
