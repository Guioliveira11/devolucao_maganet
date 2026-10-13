package com.magalu.devolucao.service;
import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.repository.PortalRepository;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AcompanhamentoServiceTest {
    final PortalRepository portal=mock(PortalRepository.class);
    final IdentidadeService identidade=mock(IdentidadeService.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-09-19T15:00:00Z"),ZoneId.of("America/Sao_Paulo"));
    final AcompanhamentoService service=new AcompanhamentoService(portal,identidade,clock);
    @Test void rejeitaPaginacaoInvalida() {
        for(var par:new int[][]{{-1,20},{0,0},{0,101}})
            assertThatThrownBy(()->service.listar(null,par[0],par[1])).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(portal);
    }
    @Test void rejeitaFiltrosIncompativeisComFila() {
        assertThatThrownBy(()->service.fila(StatusSolicitacao.CONCLUIDA,null,null,0,20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.fila(null,null,-1L,0,20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.fila(null,null,Long.MAX_VALUE,0,20)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(portal);
    }
    @Test void funcionarioSempreUsaLojaDaIdentidade() {
        var loja=Loja.builder().id(7L).ativa(true).build();
        when(identidade.funcionario()).thenReturn(Funcionario.builder().loja(loja).build());
        service.fila(null,null,2L,0,20);
        verify(portal).fila(7L,null,null,2L,0,20,LocalDateTime.now(clock));
    }
    @Test void lojaInativaNaoAcessaFila() {
        when(identidade.funcionario()).thenReturn(Funcionario.builder().loja(Loja.builder().ativa(false).build()).build());
        assertThatThrownBy(()->service.fila(null,null,null,0,20)).hasMessage("Loja inativa");
        verifyNoInteractions(portal);
    }
}
