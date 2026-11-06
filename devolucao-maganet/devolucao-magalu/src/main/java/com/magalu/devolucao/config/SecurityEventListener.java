package com.magalu.devolucao.config;

import com.magalu.devolucao.repository.ContaAcessoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SecurityEventListener {

    private static final int MAX_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;

    private final ContaAcessoRepository contas;
    private final Clock clock;

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        String login = event.getAuthentication().getName();
        
        contas.findByLogin(login).ifPresent(conta -> {
            conta.registrarSucesso();
            contas.save(conta);
        });
    }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String login = event.getAuthentication().getName();
        
        contas.findByLogin(login).ifPresent(conta -> {
            conta.registrarFalha(LocalDateTime.now(clock), MAX_TENTATIVAS, MINUTOS_BLOQUEIO);
            contas.save(conta);
        });
    }
}
