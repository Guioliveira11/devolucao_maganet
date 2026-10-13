package com.magalu.devolucao.controller;

import com.magalu.devolucao.service.LojaProximaService;
import com.magalu.devolucao.repository.LojaRepository;
import com.magalu.devolucao.exception.GlobalExceptionHandler;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LojaControllerTest {
    @Test void parametrosInvalidosRetornam400ComMensagem() throws Exception {
        var repo = mock(LojaRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        var mvc = MockMvcBuilders.standaloneSetup(new LojaController(new LojaProximaService(repo),
                new com.magalu.devolucao.service.LojaConsultaService(repo)))
                .setControllerAdvice(new GlobalExceptionHandler(clock)).build();
        for (var latitude : new String[]{"91", "NaN", "Infinity"}) {
            mvc.perform(get("/api/v1/lojas/proximas").param("latitude",latitude).param("longitude","0"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").isNotEmpty());
        }
        verifyNoInteractions(repo);
    }
}
