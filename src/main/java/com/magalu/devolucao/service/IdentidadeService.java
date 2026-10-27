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

    // Cada conta pertence a exatamente um perfil: cliente, funcionario ou ONG.
    private static int vinculos(ContaAcesso conta) {
        return (conta.getUsuario() != null ? 1 : 0)
                + (conta.getFuncionario() != null ? 1 : 0)
                + (conta.getOng() != null ? 1 : 0);
    }

    public Usuario cliente() {
        var conta = atual();
        if (conta.getUsuario() == null || vinculos(conta) != 1)
            throw new AccessDeniedException("Acao exclusiva de cliente");
        return conta.getUsuario();
    }

    public Funcionario funcionario() {
        var conta = atual();
        if (conta.getFuncionario() == null || vinculos(conta) != 1)
            throw new AccessDeniedException("Acao exclusiva de funcionario");
        return conta.getFuncionario();
    }

    public OngParceira ong() {
        var conta = atual();
        if (conta.getOng() == null || vinculos(conta) != 1)
            throw new AccessDeniedException("Acao exclusiva de ONG");
        if (!conta.getOng().isAtiva()) throw new AccessDeniedException("ONG inativa");
        return conta.getOng();
    }

    public com.magalu.devolucao.dto.SessaoResponse sessao() {
        var conta = atual();
        if (vinculos(conta) != 1) throw new AccessDeniedException("Conta sem vinculo valido");
        if (conta.getUsuario() != null)
            return new com.magalu.devolucao.dto.SessaoResponse(conta.getUsuario().getNome(), "CLIENTE", null, null);
        if (conta.getOng() != null)
            return new com.magalu.devolucao.dto.SessaoResponse(conta.getOng().getNome(), "ONG", null, null);
        var funcionario = conta.getFuncionario();
        return new com.magalu.devolucao.dto.SessaoResponse(funcionario.getNome(), "FUNCIONARIO",
                funcionario.getLoja().getId(), funcionario.getLoja().getNome());
    }
}
