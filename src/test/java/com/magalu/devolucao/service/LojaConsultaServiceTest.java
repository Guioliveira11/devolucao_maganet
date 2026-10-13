package com.magalu.devolucao.service;
import com.magalu.devolucao.domain.Loja;
import com.magalu.devolucao.repository.LojaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class LojaConsultaServiceTest {
    @Test void buscaPaginadaPorNomeOuEndereco(){
        var repo=mock(LojaRepository.class);
        var pageable=PageRequest.of(1,10,Sort.by("nome","id"));
        when(repo.findByAtivaTrueAndNomeContainingIgnoreCaseOrAtivaTrueAndEnderecoContainingIgnoreCase("Centro","Centro",pageable))
                .thenReturn(new PageImpl<>(List.of(Loja.builder().id(9L).nome("Loja Centro").endereco("Rua A").build()),pageable,11));
        var page=new LojaConsultaService(repo).listar(" Centro ",1,10);
        assertThat(page.total()).isEqualTo(11);
        assertThat(page.itens()).singleElement().satisfies(l->assertThat(l.nome()).isEqualTo("Loja Centro"));
    }
    @Test void rejeitaBuscaOuPaginaInvalida(){
        var repo=mock(LojaRepository.class);var service=new LojaConsultaService(repo);
        assertThatThrownBy(()->service.listar("x".repeat(121),0,20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.listar("",0,101)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }
}
