package com.magalu.devolucao.controller;

import com.magalu.devolucao.exception.*;
import com.magalu.devolucao.service.AcompanhamentoService;
import java.time.Clock;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
class ErrosApiTest {
    @RestController static class Falhas {
        @GetMapping("/falha/{tipo}") void falhar(@PathVariable String tipo) {
            switch(tipo) {
                case "negocio" -> throw new RegraNegocioException("Loja inativa");
                case "parametro" -> throw new ParametroInvalidoException("Pagina invalida");
                case "qr" -> throw new FalhaGeracaoQRCodeException(new IOException("DETALHE_INTERNO"));
                case "argumento" -> throw new IllegalArgumentException("DETALHE_INTERNO");
                default -> throw new IllegalStateException("DETALHE_INTERNO");
            }
        }
    }
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new Falhas(),
            new AcompanhamentoController(mock(AcompanhamentoService.class)))
            .setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC())).build();

    @Test void falhasTecnicasSao500SemDetalhesNaRespostaEComLog(CapturedOutput output) throws Exception {
        for(var tipo:new String[]{"qr","estado","argumento"}) {
            mvc.perform(get("/falha/"+tipo)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensagem").value("Erro interno ao processar a solicitacao."))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DETALHE_INTERNO"))));
        }
        assertThat(output.getOut()).contains("Falha interna ao processar requisicao", "DETALHE_INTERNO");
    }
    @Test void regraDeNegocioContinua409() throws Exception {
        mvc.perform(get("/falha/negocio")).andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Loja inativa"));
    }
    @Test void parametroDaAplicacaoContinua400() throws Exception {
        mvc.perform(get("/falha/parametro")).andExpect(status().isBadRequest());
    }
    @Test void errosDoSpringNaoViram500() throws Exception {
        mvc.perform(get("/api/v1/devolucoes").param("status","INEXISTENTE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/devolucoes").param("pagina","abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/devolucoes")).andExpect(status().isMethodNotAllowed());
    }
}
