package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.ContaAcesso;
import com.magalu.devolucao.domain.RedefinicaoSenha;
import com.magalu.devolucao.exception.ParametroInvalidoException;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import com.magalu.devolucao.repository.RedefinicaoSenhaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Recuperacao de senha por e-mail.
 * - O pedido sempre responde igual, exista ou nao uma conta com o e-mail (nao revela quem e cliente).
 * - O token tem 256 bits aleatorios, vale 30 minutos, so funciona uma vez e so o hash fica no banco.
 * - No maximo 3 pedidos por hora por conta, e so o link mais recente vale.
 */
@Service
public class RedefinicaoSenhaService {

    public interface EnvioEmailService {
        boolean configurado();

        boolean enviar(String destinatario, String assunto, String texto);
    }

    private static final Logger log = LoggerFactory.getLogger(RedefinicaoSenhaService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final int MAX_PEDIDOS_POR_HORA = 3;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ContaAcessoRepository contas;
    private final RedefinicaoSenhaRepository redefinicoes;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final EnvioEmailService email;
    private final String baseUrl;

    /** O envio acontece fora da requisicao, para o tempo de resposta nao revelar se o e-mail existe. */
    private final ExecutorService envio = Executors.newSingleThreadExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "redefinicao-senha-email");
        thread.setDaemon(true);
        return thread;
    });

    public RedefinicaoSenhaService(ContaAcessoRepository contas, RedefinicaoSenhaRepository redefinicoes,
                                   PasswordEncoder encoder, Clock clock, EnvioEmailService email,
                                   @Value("${APP_BASE_URL:http://localhost:8080}") String baseUrl) {
        this.contas = contas;
        this.redefinicoes = redefinicoes;
        this.encoder = encoder;
        this.clock = clock;
        this.email = email;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional
    public void solicitar(String emailInformado) {
        String endereco = emailInformado == null ? "" : emailInformado.trim().toLowerCase();
        if (endereco.length() > 255 || !EMAIL.matcher(endereco).matches()) return;

        ContaAcesso conta = contas.findByEmailIgnoreCase(endereco).filter(ContaAcesso::isAtiva).orElse(null);
        if (conta == null) return;

        LocalDateTime agora = LocalDateTime.now(clock);
        if (redefinicoes.countByConta_IdAndCriadoEmAfter(conta.getId(), agora.minusHours(1)) >= MAX_PEDIDOS_POR_HORA)
            return;

        redefinicoes.invalidarPendentes(conta.getId(), agora);
        String token = novoToken();
        redefinicoes.save(RedefinicaoSenha.builder().conta(conta).tokenHash(hash(token))
                .criadoEm(agora).expiraEm(agora.plus(VALIDADE)).build());

        // O token vai no fragmento (#), que o navegador nao envia ao servidor.
        String link = baseUrl + "/#redefinir-senha=" + token;
        String login = conta.getLogin();
        envio.execute(() -> entregar(endereco, login, link));
    }

    @Transactional
    public void confirmar(String token, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 8 || novaSenha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ParametroInvalidoException("A senha deve ter de 8 a 72 caracteres.");

        String hashDoToken = hash(token == null ? "" : token.trim());
        LocalDateTime agora = LocalDateTime.now(clock);
        if (redefinicoes.consumir(hashDoToken, agora) != 1) throw linkInvalido();

        ContaAcesso conta = redefinicoes.findByTokenHash(hashDoToken).orElseThrow(this::linkInvalido).getConta();
        conta.setSenhaHash(encoder.encode(novaSenha));
        conta.registrarSucesso(); // zera tentativas falhas e qualquer bloqueio de login
        redefinicoes.invalidarPendentes(conta.getId(), agora);
    }

    private ParametroInvalidoException linkInvalido() {
        return new ParametroInvalidoException("Link inválido ou expirado. Peça um novo link de recuperação.");
    }

    private void entregar(String destinatario, String login, String link) {
        if (!email.configurado()) {
            // Ambiente sem provedor de e-mail: o link aparece so no log do servidor (quem tem acesso ao Render).
            log.warn("[REDEFINICAO-SENHA] E-mail nao configurado (EMAIL_PROVIDER). Link para o login '{}': {}", login, link);
            return;
        }
        String texto = "Olá!\n\n"
                + "Recebemos um pedido para redefinir a senha da conta \"" + login + "\" no Maga Next.\n\n"
                + "Para criar uma nova senha, abra o link abaixo. Ele vale por 30 minutos e só pode ser usado uma vez:\n"
                + link + "\n\n"
                + "Se não foi você, ignore este e-mail: sua senha continua a mesma.\n";
        if (!email.enviar(destinatario, "Redefinição de senha - Maga Next", texto))
            log.error("Nao foi possivel enviar o e-mail de redefinicao para o login '{}'", login);
    }

    private static String novoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
