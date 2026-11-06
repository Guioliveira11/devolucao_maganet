package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.*;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentidadeService {
    private final ContaAcessoRepository contas;

    private ContaAcesso atual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Autenticacao obrigatoria");
        var conta = contas.findByLogin(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Conta nao encontrada"));
        if (!conta.isAtiva()) throw new AccessDeniedException("Conta inativa");
        return conta;
    }

    public Usuario cliente() {
        var conta = atual();
        if (conta.getUsuario() == null || conta.getFuncionario() != null)
            throw new AccessDeniedException("Acao exclusiva de cliente");
        return conta.getUsuario();
    }

    public Funcionario funcionario() {
        var conta = atual();
        if (conta.getFuncionario() == null || conta.getUsuario() != null)
            throw new AccessDeniedException("Acao exclusiva de funcionario");
        return conta.getFuncionario();
    }

    public com.magalu.devolucao.dto.SessaoResponse sessao() {
        var conta = atual();
        if ((conta.getUsuario() == null) == (conta.getFuncionario() == null))
            throw new AccessDeniedException("Conta sem vinculo valido");
        if (conta.getUsuario() != null)
            return new com.magalu.devolucao.dto.SessaoResponse(conta.getUsuario().getNome(), "CLIENTE", null, null);
        var funcionario = conta.getFuncionario();
        return new com.magalu.devolucao.dto.SessaoResponse(funcionario.getNome(), "FUNCIONARIO",
                funcionario.getLoja().getId(), funcionario.getLoja().getNome());
    }
}
