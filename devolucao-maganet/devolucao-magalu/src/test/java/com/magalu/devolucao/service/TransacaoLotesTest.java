package com.magalu.devolucao.service;

import com.magalu.devolucao.config.*;
import com.magalu.devolucao.repository.NotificacaoRepository;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class TransacaoLotesTest {
    static class ContadorTransacoes extends AbstractPlatformTransactionManager {
        int iniciadas, commits, rollbacks;
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object tx,TransactionDefinition def){iniciadas++;}
        protected void doCommit(DefaultTransactionStatus status){commits++;}
        protected void doRollback(DefaultTransactionStatus status){rollbacks++;}
    }

    @Test void falhaNoSegundoLoteNaoDesfazCommitDoPrimeiro() {
        var clock=Clock.fixed(Instant.parse("2026-09-26T15:00:00Z"),ZoneId.of("America/Sao_Paulo"));
        var agora=LocalDateTime.now(clock);
        var repo=mock(NotificacaoRepository.class);
        var tx=new ContadorTransacoes();
        var factory=new ProxyFactory(new NotificacaoService(repo,mock(IdentidadeService.class),clock));
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));
        var service=(NotificacaoService)factory.getProxy();
        var a=new NotificacaoRepository.Lembrete(1,1,1,agora.plusMinutes(30));
        var b=new NotificacaoRepository.Lembrete(2,2,1,agora.plusMinutes(40));
        when(repo.bloquearLembretes(any(),any(),eq(1))).thenReturn(List.of(a),List.of(b));
        when(repo.lembreteAindaValido(any(),any())).thenReturn(true);
        when(repo.inserir(anyLong(),anyLong(),anyString(),anyString(),anyString(),any(),any(),eq(true)))
                .thenReturn(1).thenThrow(new IllegalStateException("falha-no-segundo-lote"));
        var config=new NotificacaoProperties();config.setTamanhoLote(1);
        assertThatThrownBy(()->new NotificacaoJob(service,config).lembrar()).hasMessage("falha-no-segundo-lote");
        assertThat(tx.iniciadas).isEqualTo(2);
        assertThat(tx.commits).isEqualTo(1);
        assertThat(tx.rollbacks).isEqualTo(1);
    }
}
