package com.magalu.devolucao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Envio de e-mail por API HTTPS (Brevo ou Resend).
 * Nao usa SMTP de proposito: o plano gratuito do Render bloqueia as portas SMTP.
 *
 * Variaveis: EMAIL_PROVIDER (brevo | resend), EMAIL_API_KEY, EMAIL_FROM, EMAIL_FROM_NAME.
 * Sem elas, configurado() devolve false e nada e enviado.
 */
@Service
public class EnvioEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnvioEmailService.class);

    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String provedor;
    private final String chaveApi;
    private final String remetente;
    private final String nomeRemetente;

    public EnvioEmailService(ObjectMapper json,
                             @Value("${EMAIL_PROVIDER:}") String provedor,
                             @Value("${EMAIL_API_KEY:}") String chaveApi,
                             @Value("${EMAIL_FROM:}") String remetente,
                             @Value("${EMAIL_FROM_NAME:Maga Next}") String nomeRemetente) {
        this.json = json;
        this.provedor = provedor.trim().toLowerCase(Locale.ROOT);
        this.chaveApi = chaveApi.trim();
        this.remetente = remetente.trim();
        this.nomeRemetente = nomeRemetente.trim();
    }

    public boolean configurado() {
        return !provedor.isEmpty() && !chaveApi.isEmpty() && !remetente.isEmpty();
    }

    /** Envia um e-mail de texto simples. Nunca lanca excecao: falhas vao para o log. */
    public boolean enviar(String para, String assunto, String texto) {
        if (!configurado()) return false;
        try {
            HttpRequest requisicao = switch (provedor) {
                case "brevo" -> requisicaoBrevo(para, assunto, texto);
                case "resend" -> requisicaoResend(para, assunto, texto);
                default -> throw new IllegalStateException("EMAIL_PROVIDER invalido: use brevo ou resend");
            };
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() / 100 != 2) {
                log.error("Envio de e-mail recusado pelo provedor {} (HTTP {}): {}",
                        provedor, resposta.statusCode(), resumo(resposta.body()));
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Envio de e-mail interrompido");
            return false;
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail via {}: {}", provedor, e.toString());
            return false;
        }
    }

    private HttpRequest requisicaoBrevo(String para, String assunto, String texto) throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "sender", Map.of("name", nomeRemetente, "email", remetente),
                "to", List.of(Map.of("email", para)),
                "subject", assunto,
                "textContent", texto));
        return HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", chaveApi)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("user-agent", "maga-next/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();
    }

    private HttpRequest requisicaoResend(String para, String assunto, String texto) throws Exception {
        String corpo = json.writeValueAsString(Map.of(
                "from", nomeRemetente + " <" + remetente + ">",
                "to", List.of(para),
                "subject", assunto,
                "text", texto));
        return HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15))
                .header("authorization", "Bearer " + chaveApi)
                .header("content-type", "application/json")
                .header("user-agent", "maga-next/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();
    }

    private static String resumo(String corpo) {
        if (corpo == null) return "";
        return corpo.length() > 300 ? corpo.substring(0, 300) + "..." : corpo;
    }
}
