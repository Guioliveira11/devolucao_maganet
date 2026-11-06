package com.magalu.devolucao.controller;

import com.magalu.devolucao.config.*;
import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import com.magalu.devolucao.service.IdentidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({SessaoController.class,CsrfController.class})
@Import({SecurityConfig.class,TempoConfig.class,IdentidadeService.class})
class SessaoWebTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockBean ContaAcessoRepository contas;
    ContaAcesso conta;
    @Test void recursosDoSiteSaoPublicosMasDadosExigemLogin() throws Exception {
        for (String path : new String[]{"/index.html", "/app.js", "/app.css", "/vendor/jsQR.js", "/fonts/Poppins-Regular.ttf", "/fonts/Poppins-Bold.ttf"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/itens")).andExpect(status().isUnauthorized());
    }
    @BeforeEach void preparar(){
        conta=ContaAcesso.builder().login("cliente").senhaHash(encoder.encode("senha-teste"))
                .usuario(Usuario.builder().id(1L).nome("Cliente Teste").build()).build();
        when(contas.findByLogin("cliente")).thenAnswer(a->Optional.of(conta));
    }
    MockHttpSession entrar() throws Exception {
        var r=mvc.perform(post("/api/v1/sessao").with(csrf()).param("username","cliente").param("password","senha-teste"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession)r.getRequest().getSession(false);
    }
    @Test void loginCriaSessaoEPerfilSemExporCredenciais() throws Exception {
        var session=entrar();assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/sessao").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(header().string("Content-Security-Policy",org.hamcrest.Matchers.containsString("script-src 'self'")));
    }
    @Test void naoAutenticadoPodeObterCsrfMasNaoOPerfil() throws Exception {
        mvc.perform(get("/api/v1/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/v1/sessao")).andExpect(status().isUnauthorized());
    }
    @Test void loginExigeCsrf() throws Exception {
        mvc.perform(post("/api/v1/sessao").param("username","cliente").param("password","senha-teste"))
                .andExpect(status().isForbidden());
    }
    @Test void senhaIncorretaRetorna401SemRedirecionar() throws Exception {
        mvc.perform(post("/api/v1/sessao").with(csrf()).param("username","cliente").param("password","errada"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensagem").exists())
                .andExpect(header().doesNotExist("Location"));
    }
    @Test void logoutInvalidaSessao() throws Exception {
        var session=entrar();
        mvc.perform(post("/api/v1/sessao/sair").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/sessao")).andExpect(status().isUnauthorized());
    }
    @Test void contaDesativadaNaoConsultaPerfilMesmoComSessaoAntiga() throws Exception {
        var session=entrar();conta.setAtiva(false);
        mvc.perform(get("/api/v1/sessao").session(session)).andExpect(status().isForbidden());
    }
    @Test void perfilFuncionarioUsaLojaDaConta() throws Exception {
        conta.setUsuario(null);
        conta.setFuncionario(Funcionario.builder().nome("Atendente").loja(Loja.builder().id(7L).nome("Loja Centro").build()).build());
        mvc.perform(get("/api/v1/sessao").session(entrar())).andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.lojaId").value(7));
    }
}
