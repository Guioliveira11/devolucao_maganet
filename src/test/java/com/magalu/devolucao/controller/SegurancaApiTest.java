package com.magalu.devolucao.controller;

import com.magalu.devolucao.config.SecurityConfig;
import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import com.magalu.devolucao.service.DevolucaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DevolucaoController.class, FuncionarioController.class, ItemCompradoController.class, CsrfController.class,
        AcompanhamentoController.class, NotificacaoController.class})
@Import({SecurityConfig.class, com.magalu.devolucao.config.TempoConfig.class})
class SegurancaApiTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockBean DevolucaoService service;
    @MockBean ContaAcessoRepository contas;
    @MockBean com.magalu.devolucao.service.AcompanhamentoService acompanhamento;
    @MockBean com.magalu.devolucao.service.NotificacaoService notificacoes;
    @Test void anonimoRecebe401() throws Exception {
        mvc.perform(get("/api/v1/itens")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void clienteNaoPodeEscanearMesmoEnviandoFuncionarioId() throws Exception {
        mvc.perform(post("/api/v1/funcionario/devolucoes/escanear")
                .with(user("c").roles("CLIENTE")).with(csrf()).contentType("application/json")
                .content("{\"token\":\""+UUID.randomUUID()+"\",\"funcionarioId\":99}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void escritaSemCsrfBloqueada() throws Exception {
        mvc.perform(post("/api/v1/devolucoes/1/cancelar").with(user("c").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void funcionarioNaoPodeListarItensDeCliente() throws Exception {
        mvc.perform(get("/api/v1/itens").with(user("f").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
    }
    @Test void rotasDoClienteChamamServico() throws Exception {
        var auth=user("c").roles("CLIENTE");
        mvc.perform(get("/api/v1/itens").with(auth)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/devolucoes/1/qrcode/renovar").with(auth).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/devolucoes/1/cancelar").with(auth).with(csrf())).andExpect(status().isOk());
        verify(service).listarItens(0,20); verify(service).renovarQRCode(1L); verify(service).cancelar(1L);
    }
    @Test void funcionarioIdNoJsonNaoDefineAutor() throws Exception {
        var token=UUID.randomUUID();
        mvc.perform(post("/api/v1/funcionario/devolucoes/escanear").with(user("f").roles("FUNCIONARIO"))
                .with(csrf()).contentType("application/json")
                .content("{\"token\":\""+token+"\",\"funcionarioId\":999}"))
                .andExpect(status().isOk());
        verify(service).processarEscaneamento(token);
    }
    @Test void loginRealComSenhaBcrypt() throws Exception {
        when(contas.findByLogin("cliente")).thenReturn(Optional.of(ContaAcesso.builder()
                .login("cliente").senhaHash(encoder.encode("senha-teste"))
                .usuario(Usuario.builder().id(1L).build()).build()));
        mvc.perform(get("/api/v1/itens").with(httpBasic("cliente","senha-teste"))).andExpect(status().isOk());
    }
    @Test void senhaErradaNaoAutentica() throws Exception {
        when(contas.findByLogin("cliente")).thenReturn(Optional.of(ContaAcesso.builder()
                .login("cliente").senhaHash(encoder.encode("senha-teste"))
                .usuario(Usuario.builder().id(1L).build()).build()));
        mvc.perform(get("/api/v1/itens").with(httpBasic("cliente","incorreta"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void rotaCsrfRetornaToken() throws Exception {
        mvc.perform(get("/api/v1/csrf").with(user("c").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void clienteNaoAcessaFilaDaLoja() throws Exception {
        mvc.perform(get("/api/v1/funcionario/devolucoes").with(user("c").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(acompanhamento);
    }
    @Test void funcionarioNaoAcessaNotificacoesOuAcompanhamentoCliente() throws Exception {
        mvc.perform(get("/api/v1/notificacoes").with(user("f").roles("FUNCIONARIO"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/devolucoes").with(user("f").roles("FUNCIONARIO"))).andExpect(status().isForbidden());
        verifyNoInteractions(acompanhamento,notificacoes);
    }
    @Test void preferenciasELeituraExigemCsrf() throws Exception {
        mvc.perform(put("/api/v1/notificacoes/preferencias").with(user("c").roles("CLIENTE"))
                .contentType("application/json").content("{\"atualizacoes\":true,\"lembretesQr\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notificacoes/1/ler").with(user("c").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificacoes);
    }
    @Test void preferenciasIncompletasSaoRejeitadas() throws Exception {
        mvc.perform(put("/api/v1/notificacoes/preferencias").with(user("c").roles("CLIENTE")).with(csrf())
                .contentType("application/json").content("{\"atualizacoes\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notificacoes);
    }
}
