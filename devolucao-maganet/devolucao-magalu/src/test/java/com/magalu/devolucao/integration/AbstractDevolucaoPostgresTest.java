package com.magalu.devolucao.integration;

import com.magalu.devolucao.service.DevolucaoService;
import com.magalu.devolucao.repository.SolicitacaoDevolucaoRepository;
import com.magalu.devolucao.domain.enums.StatusSolicitacao;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="app.notificacoes.job-ativo=false")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
abstract class AbstractDevolucaoPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DevolucaoService service;
    @Autowired SolicitacaoDevolucaoRepository solicitacoes;
    @Autowired com.magalu.devolucao.repository.LojaRepository lojas;
    @Autowired com.magalu.devolucao.service.AcompanhamentoService acompanhamento;
    @Autowired com.magalu.devolucao.service.NotificacaoService notificacoes;
    @Autowired com.magalu.devolucao.repository.NotificacaoRepository notificacaoRepo;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired java.time.Clock clock;
    @Autowired org.springframework.transaction.PlatformTransactionManager tx;
    @BeforeEach void preparar() {
        jdbc.execute("TRUNCATE contas_acesso, registros_auditoria, qrcodes_devolucao, solicitacoes_devolucao, itens_comprados, pedidos, funcionarios, lojas, produtos, usuarios, ongs_parceiras RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO usuarios(id,nome,cpf,email) VALUES (1,'Cliente','00000000001','cliente@teste.local')");
        jdbc.update("INSERT INTO lojas(id,nome,latitude,longitude,ativa) VALUES (1,'Loja',0,0,true)");
        jdbc.update("INSERT INTO produtos(id,sku,nome,elegivel_reparo) VALUES (1,'SKU','Produto',true)");
        jdbc.update("INSERT INTO pedidos(id,numero_pedido,data_compra,usuario_id) VALUES (1,'P1',CURRENT_TIMESTAMP,1)");
        jdbc.update("INSERT INTO itens_comprados(id,produto_id,pedido_id,quantidade,data_limite_devolucao) VALUES (1,1,1,1,CURRENT_DATE+7)");
        jdbc.update("INSERT INTO contas_acesso(login,senha_hash,usuario_id) VALUES ('cliente','hash-nao-usado-neste-teste',1)");
        autenticar();
    }
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente",null,List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    @Test void criaConsultaCancelaEPermiteNovaSolicitacao() {
        var primeira=service.criarSolicitacao(1L,1L);
        assertThat(primeira.qrCodeImagemBase64()).isNotBlank();
        assertThat(service.buscarPorId(primeira.id()).id()).isEqualTo(primeira.id());
        assertThat(service.listarItens(0,20).itens().get(0).elegivelParaDevolucao()).isFalse();
        service.cancelar(primeira.id());
        assertThat(service.listarItens(0,20).itens().get(0).elegivelParaDevolucao()).isTrue();
        assertThat(service.criarSolicitacao(1L,1L).id()).isNotEqualTo(primeira.id());
    }
    @Test void somenteUmaCriacaoConcorrentePodeVencer() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        var inicio=new CountDownLatch(1);
        Callable<Boolean> criar=() -> {
            autenticar();
            try { inicio.await(); service.criarSolicitacao(1L,1L); return true; }
            catch (com.magalu.devolucao.exception.RegraNegocioException e) {
                if (!e.getMessage().contains("aberta ou concluida")) throw e;
                return false;
            } finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var a=pool.submit(criar); var b=pool.submit(criar); inicio.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            assertThat(solicitacoes.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void indiceDoBancoTambemImpedeDuplicidadeDireta() {
        service.criarSolicitacao(1L,1L);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO solicitacoes_devolucao(usuario_id,item_comprado_id,loja_id,status,data_criacao) VALUES (1,1,1,'AGUARDANDO_ESCANEAMENTO',CURRENT_TIMESTAMP)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void renovaVencidoSemQueHistoricoInvalideNovo() {
        var primeira=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE solicitacao_id=?",primeira.id());
        var renovada=service.renovarQRCode(primeira.id());
        assertThat(renovada.status()).isEqualTo(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO);
        assertThat(renovada.qrCodeImagemBase64()).isNotEqualTo(primeira.qrCodeImagemBase64());
        assertThat(solicitacoes.findIdsComQrVencido(StatusSolicitacao.AGUARDANDO_ESCANEAMENTO,
                java.time.LocalDateTime.now())).isEmpty();
    }

    @Test void filtraLojasNoBancoInclusiveAntimeridiano() {
        jdbc.update("INSERT INTO lojas(id,nome,latitude,longitude,ativa) VALUES (2,'Perto',0,0.1,true),(3,'Longe',0,5,true),(4,'Inativa',0,0,false),(5,'Antimeridiano',0,-179.99,true)");
        assertThat(lojas.findProximas(0,0,20)).extracting(com.magalu.devolucao.domain.Loja::getId)
                .containsExactlyInAnyOrder(1L,2L);
        assertThat(lojas.findProximas(0,179.99,5)).extracting(com.magalu.devolucao.domain.Loja::getId)
                .containsExactly(5L);
    }

    @Test void persisteOngEDestinoSemConcluirDevolucao() {
        var criada = service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE produtos SET categoria='livros' WHERE id=1");
        jdbc.update("INSERT INTO ongs_parceiras(id,nome,cnpj) VALUES (1,'ONG','00000000000001')");
        jdbc.update("INSERT INTO ong_categorias_aceitas(ong_id,categoria) VALUES (1,'livros')");
        jdbc.update("INSERT INTO funcionarios(id,nome,matricula,loja_id) VALUES (1,'Funcionario','F1',1)");
        jdbc.update("INSERT INTO contas_acesso(login,senha_hash,funcionario_id) VALUES ('funcionario','hash',1)");
        var token = jdbc.queryForObject("SELECT token FROM qrcodes_devolucao WHERE solicitacao_id=?", java.util.UUID.class, criada.id());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "funcionario",null,List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))));
        service.processarEscaneamento(token);
        var resposta = service.definirDestinoFinal(criada.id(),com.magalu.devolucao.domain.enums.DestinoProduto.DOACAO_ONG);
        assertThat(resposta.status()).isEqualTo(StatusSolicitacao.DESTINO_PENDENTE);
        assertThat(resposta.ongDestinatariaId()).isEqualTo(1L);
        assertThat(resposta.dataDefinicaoDestino()).isNotNull();
        var registro = jdbc.queryForMap("SELECT ong_destinataria_id, status, data_conclusao FROM solicitacoes_devolucao WHERE id=?",criada.id());
        assertThat(registro.get("ong_destinataria_id")).isEqualTo(1L);
        assertThat(registro.get("status")).isEqualTo("DESTINO_PENDENTE");
        assertThat(registro.get("data_conclusao")).isNull();
    }

    void funcionario(long lojaId) {
        jdbc.update("INSERT INTO funcionarios(id,nome,matricula,loja_id) VALUES (1,'Funcionario','F1',?) ON CONFLICT(id) DO UPDATE SET loja_id=excluded.loja_id",lojaId);
        jdbc.update("INSERT INTO contas_acesso(login,senha_hash,funcionario_id) VALUES ('funcionario','hash',1) ON CONFLICT(login) DO NOTHING");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "funcionario",null,List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))));
    }
    void outroCliente() {
        jdbc.update("INSERT INTO usuarios(id,nome,cpf,email) VALUES (2,'Outro','00000000002','outro@teste.local')");
        jdbc.update("INSERT INTO contas_acesso(login,senha_hash,usuario_id) VALUES ('outro','hash',2)");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "outro",null,List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
    }
    java.util.UUID token(long id) {
        return jdbc.queryForObject("SELECT token FROM qrcodes_devolucao WHERE solicitacao_id=? AND status='ATIVO' ORDER BY id DESC LIMIT 1", java.util.UUID.class,id);
    }
    java.time.LocalDateTime agora(){return java.time.LocalDateTime.now(clock);}

    @Test void acompanhamentoPaginadoFiltraStatusEIsolaCliente() {
        var a=service.criarSolicitacao(1L,1L);service.cancelar(a.id());
        var b=service.criarSolicitacao(1L,1L);
        var pagina=acompanhamento.listar(null,0,1);
        assertThat(pagina.total()).isEqualTo(2);
        assertThat(pagina.itens()).extracting(com.magalu.devolucao.dto.AcompanhamentoResponse::id).containsExactly(b.id());
        assertThat(acompanhamento.listar(StatusSolicitacao.CANCELADA,0,20).itens())
                .extracting(com.magalu.devolucao.dto.AcompanhamentoResponse::id).containsExactly(a.id());
        var detalhe=acompanhamento.detalhe(b.id());
        assertThat(detalhe.produto()).isEqualTo("Produto");
        assertThat(detalhe.lojaId()).isEqualTo(1L);
        assertThat(detalhe.qrExpiraEm()).isNotNull();
        outroCliente();
        assertThat(acompanhamento.listar(null,0,20).total()).isZero();
        assertThatThrownBy(()->acompanhamento.detalhe(b.id())).isInstanceOf(com.magalu.devolucao.exception.RecursoNaoEncontradoException.class);
        assertThatThrownBy(()->acompanhamento.historico(b.id(),0,20)).isInstanceOf(com.magalu.devolucao.exception.RecursoNaoEncontradoException.class);
    }

    @Test void historicoNaoExpoeAuditoriaInterna() throws Exception {
        var a=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE registros_auditoria SET autor='SEGREDO_AUTOR',detalhes='SEGREDO_DETALHES'");
        jdbc.update("INSERT INTO registros_auditoria(solicitacao_id,acao,timestamp,detalhes) VALUES (?,'INTERNO',CURRENT_TIMESTAMP,'SEGREDO')",a.id());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/devolucoes/"+a.id()+"/historico")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("cliente").roles("CLIENTE")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.itens[0].autor").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO"))));
    }

    @Test void filaUsaRecebimentoEIsolaLoja() {
        var a=service.criarSolicitacao(1L,1L);funcionario(1);
        service.processarEscaneamento(token(a.id()));
        assertThat(acompanhamento.fila(null,null,null,0,20).itens()).singleElement().satisfies(i->{
            assertThat(i.recebidaEm()).isNotNull();assertThat(i.minutosDesdeRecebimento()).isZero();
        });
        jdbc.update("UPDATE solicitacoes_devolucao SET data_recebimento=? WHERE id=?",agora().minusHours(3),a.id());
        assertThat(acompanhamento.fila(StatusSolicitacao.EM_ANALISE,null,2L,0,20).total()).isEqualTo(1);
        assertThat(acompanhamento.fila(null,null,4L,0,20).total()).isZero();
        service.definirDestinoFinal(a.id(),com.magalu.devolucao.domain.enums.DestinoProduto.ESTOQUE);
        assertThat(acompanhamento.fila(StatusSolicitacao.EM_ANALISE,null,null,0,20).total()).isZero();
        assertThat(acompanhamento.fila(StatusSolicitacao.DESTINO_PENDENTE,com.magalu.devolucao.domain.enums.DestinoProduto.ESTOQUE,null,0,20).total()).isEqualTo(1);
        jdbc.update("INSERT INTO lojas(id,nome,latitude,longitude) VALUES (2,'Outra',1,1)");
        funcionario(2);
        assertThat(acompanhamento.fila(null,null,null,0,20).total()).isZero();
    }

    @Test void notificacoesSaoPrivadasELeituraEhIdempotente() {
        var a=service.criarSolicitacao(1L,1L);
        var n=notificacoes.listar(true,0,20).itens().get(0);
        assertThat(n.solicitacaoId()).isEqualTo(a.id());
        notificacoes.marcarLida(n.id());
        var primeiraLeitura=notificacoes.listar(false,0,20).itens().get(0).lidaEm();
        notificacoes.marcarLida(n.id());
        assertThat(notificacoes.listar(false,0,20).itens().get(0).lidaEm()).isEqualTo(primeiraLeitura);
        assertThat(notificacoes.listar(true,0,20).total()).isZero();
        outroCliente();
        assertThat(notificacoes.listar(false,0,20).total()).isZero();
        assertThatThrownBy(()->notificacoes.marcarLida(n.id())).isInstanceOf(com.magalu.devolucao.exception.RecursoNaoEncontradoException.class);
    }

    @Test void lembreteUnicoPorQrERenovacaoGeraNovoLembrete() {
        var a=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().plusMinutes(30),a.id());
        notificacoes.gerarLembretes(120,200);notificacoes.gerarLembretes(120,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isEqualTo(1);
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().minusMinutes(1),a.id());
        service.renovarQRCode(a.id());
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=? AND status='ATIVO'",agora().plusMinutes(30),a.id());
        notificacoes.gerarLembretes(120,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isEqualTo(2);
    }

    @Test void preferenciasIndependentesENaoNotificaConclusaoPendente() {
        notificacoes.salvarPreferencias(new com.magalu.devolucao.dto.PreferenciasNotificacao(false,false));
        assertThat(notificacoes.preferencias().atualizacoes()).isFalse();
        var a=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().plusMinutes(30),a.id());
        notificacoes.gerarLembretes(120,200);
        assertThat(notificacoes.listar(false,0,20).total()).isZero();
        notificacoes.salvarPreferencias(new com.magalu.devolucao.dto.PreferenciasNotificacao(false,true));
        notificacoes.gerarLembretes(120,200);
        assertThat(notificacoes.listar(false,0,20).itens()).extracting(com.magalu.devolucao.dto.NotificacaoResponse::tipo).containsExactly("QR_PRESTES_A_EXPIRAR");
        notificacoes.salvarPreferencias(new com.magalu.devolucao.dto.PreferenciasNotificacao(true,true));
        funcionario(1);service.processarEscaneamento(token(a.id()));
        service.definirDestinoFinal(a.id(),com.magalu.devolucao.domain.enums.DestinoProduto.ESTOQUE);
        autenticar();
        assertThat(notificacoes.listar(false,0,20).itens()).extracting(com.magalu.devolucao.dto.NotificacaoResponse::tipo)
                .contains("QR_ESCANEADO","DESTINO_DEFINIDO").doesNotContain("CONCLUIDA");
    }

    @Test void naoLembraQrForaDaJanelaOuCancelado() {
        var a=service.criarSolicitacao(1L,1L);
        notificacoes.gerarLembretes(120,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isZero();
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().minusMinutes(1),a.id());
        notificacoes.gerarLembretes(120,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isZero();
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().plusMinutes(30),a.id());
        service.cancelar(a.id());notificacoes.gerarLembretes(120,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isZero();
    }

    @Test void rollbackNaoDeixaNotificacaoFantasma() {
        var template=new org.springframework.transaction.support.TransactionTemplate(tx);
        assertThatThrownBy(()->template.executeWithoutResult(status->{service.criarSolicitacao(1L,1L);throw new IllegalStateException("rollback-teste");}))
                .hasMessage("rollback-teste");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes",Long.class)).isZero();
        assertThat(solicitacoes.count()).isZero();
    }

    @Test void workersConcorrentesNaoDuplicamLembrete() throws Exception {
        var a=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE qrcodes_devolucao SET data_expiracao=? WHERE solicitacao_id=?",agora().plusMinutes(30),a.id());
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        Callable<com.magalu.devolucao.service.ResultadoLote> executar=()->{inicio.await();return notificacoes.gerarLembretes(120,200);};
        try {
            var x=pool.submit(executar);var y=pool.submit(executar);inicio.countDown();
            x.get(20,TimeUnit.SECONDS);y.get(20,TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void itensSaoPaginadosComElegibilidadeCorreta() {
        service.criarSolicitacao(1L,1L);
        jdbc.update("INSERT INTO itens_comprados(id,produto_id,pedido_id,quantidade,data_limite_devolucao) VALUES (2,1,1,1,CURRENT_DATE+7),(3,1,1,1,CURRENT_DATE+7)");
        var primeira=service.listarItens(0,2);
        assertThat(primeira.total()).isEqualTo(3);
        assertThat(primeira.itens()).extracting(com.magalu.devolucao.dto.ItemCompradoResponse::id).containsExactly(3L,2L);
        assertThat(primeira.itens()).allSatisfy(i->assertThat(i.elegivelParaDevolucao()).isTrue());
        assertThat(service.listarItens(1,2).itens()).singleElement().satisfies(i->{
            assertThat(i.id()).isEqualTo(1L);assertThat(i.elegivelParaDevolucao()).isFalse();
        });
        assertThat(service.listarItens(2,2).itens()).isEmpty();
        outroCliente();
        assertThat(service.listarItens(0,2).total()).isZero();
    }

    void prepararMuitosQrs(java.time.LocalDateTime expira) {
        jdbc.update("INSERT INTO itens_comprados(id,produto_id,pedido_id,quantidade,data_limite_devolucao) SELECT n,1,1,1,CURRENT_DATE+7 FROM generate_series(1001,1205) n");
        jdbc.update("INSERT INTO solicitacoes_devolucao(id,usuario_id,item_comprado_id,loja_id,status,data_criacao) SELECT n,1,n,1,'AGUARDANDO_ESCANEAMENTO',CURRENT_TIMESTAMP FROM generate_series(1001,1205) n");
        jdbc.update("INSERT INTO qrcodes_devolucao(token,data_geracao,data_expiracao,status,solicitacao_id) SELECT gen_random_uuid(),CURRENT_TIMESTAMP,?,'ATIVO',n FROM generate_series(1001,1205) n",expira);
    }

    @Test void jobProcessaMaisDeDuzentosLembretesSemDuplicar() {
        prepararMuitosQrs(agora().plusMinutes(60));
        var config=new com.magalu.devolucao.config.NotificacaoProperties();
        config.setTamanhoLote(100);
        var job=new com.magalu.devolucao.config.NotificacaoJob(notificacoes,config);
        job.lembrar();job.lembrar();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_PRESTES_A_EXPIRAR'",Long.class)).isEqualTo(205);
        assertThat(notificacoes.pendenciasLembretes(120).quantidade()).isZero();
    }

    @Autowired com.magalu.devolucao.service.QRCodeService qrCodes;
    @Test void expiracaoUsaLotesLimitadosSemDeixarPendencias() {
        prepararMuitosQrs(agora().minusMinutes(1));
        assertThat(qrCodes.expirarCodigosVencidos(100)).isEqualTo(new com.magalu.devolucao.service.ResultadoLote(100,100));
        assertThat(qrCodes.contarExpiracoesPendentes()).isEqualTo(105);
        assertThat(qrCodes.expirarCodigosVencidos(100).processados()).isEqualTo(100);
        assertThat(qrCodes.expirarCodigosVencidos(100).processados()).isEqualTo(5);
        assertThat(qrCodes.contarExpiracoesPendentes()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacoes WHERE tipo='QR_EXPIRADO'",Long.class)).isEqualTo(205);
    }

    @Test void historicoLegadoConcluidoNaoExibeMensagemDePendencia() {
        var a=service.criarSolicitacao(1L,1L);
        jdbc.update("UPDATE solicitacoes_devolucao SET status='CONCLUIDA',data_conclusao=? WHERE id=?",agora(),a.id());
        jdbc.update("INSERT INTO registros_auditoria(solicitacao_id,acao,timestamp) VALUES (?,'DESTINO_DEFINIDO',?)",a.id(),agora());
        assertThat(acompanhamento.historico(a.id(),0,20).itens())
                .filteredOn(e->e.tipo().equals("DESTINO_DEFINIDO")).singleElement()
                .satisfies(e->assertThat(e.mensagem()).isEqualTo("O destino do produto foi definido."));
    }
}
