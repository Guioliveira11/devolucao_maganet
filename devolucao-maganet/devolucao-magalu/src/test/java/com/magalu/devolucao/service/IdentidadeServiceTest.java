package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentidadeServiceTest {
    ContaAcessoRepository repo=mock(ContaAcessoRepository.class);
    IdentidadeService service=new IdentidadeService(repo);
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    void autenticar(ContaAcesso conta) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("login",null,List.of()));
        when(repo.findByLogin("login")).thenReturn(Optional.of(conta));
    }
    @Test void exigeAutenticacao() {
        assertThatThrownBy(service::cliente).isInstanceOf(AccessDeniedException.class);
    }
    @Test void resolveClientePeloLogin() {
        var usuario=Usuario.builder().id(9L).build();
        autenticar(ContaAcesso.builder().usuario(usuario).build());
        assertThat(service.cliente()).isSameAs(usuario);
    }
    @Test void clienteNaoEhFuncionario() {
        autenticar(ContaAcesso.builder().usuario(Usuario.builder().id(9L).build()).build());
        assertThatThrownBy(service::funcionario).isInstanceOf(AccessDeniedException.class);
    }
    @Test void funcionarioResolvidoPeloLogin() {
        var funcionario=Funcionario.builder().id(7L).build();
        autenticar(ContaAcesso.builder().funcionario(funcionario).build());
        assertThat(service.funcionario()).isSameAs(funcionario);
    }
    @Test void contaInativaBloqueada() {
        autenticar(ContaAcesso.builder().usuario(Usuario.builder().id(9L).build()).ativa(false).build());
        assertThatThrownBy(service::cliente).isInstanceOf(AccessDeniedException.class);
    }
}
