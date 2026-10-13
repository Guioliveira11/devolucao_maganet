# Sistema de devolução de itens — Magalu

Site responsivo para **clientes** e **funcionários de loja** gerenciarem devoluções de produtos por QR Code.
Backend em Java 17 + Spring Boot 3.3.4 + PostgreSQL 16; frontend em HTML/CSS/JS nativo, servido pelo próprio backend.

> Vai abrir o projeto em outro editor ou assistente? Comece por [INICIAR_AQUI.md](INICIAR_AQUI.md).

## Como funciona

1. O cliente escolhe um item comprado e uma loja e solicita a devolução. O sistema gera um QR Code com validade.
2. O cliente leva o item à loja e apresenta o QR.
3. O funcionário lê o QR (câmera ou código digitado) e confirma o recebimento (`EM_ANALISE`).
4. O funcionário define o destino: `ESTOQUE`, `REPARO` ou `DOACAO_ONG`. A solicitação fica em `DESTINO_PENDENTE`.

Fluxo de status: `AGUARDANDO_ESCANEAMENTO` → `EM_ANALISE` → `DESTINO_PENDENTE`.
Também existem `QR_EXPIRADO` (renovável), `CANCELADA` e `CONCLUIDA` (apenas histórico; veja [Pendências](#pendências)).

## Funcionalidades

- **Cliente:** lista paginada de compras, solicitação de devolução, busca de lojas por nome/endereço ou localização, QR de entrega, renovação do QR vencido, cancelamento antes do recebimento, acompanhamento com histórico e avisos internos.
- **Funcionário:** fila da própria loja (filtros de etapa, destino e tempo), leitura de QR e definição de destino com confirmação.
- **Avisos internos:** atualizações e lembretes de QR dentro do site, com preferências. Não há e-mail, push ou SMS.
- **Segurança:** login por sessão (cookie HttpOnly), HTTP Basic para API, CSRF, senhas BCrypt e bloqueio de conta.

## Executar

Pré-requisitos: JDK 17, Maven 3.9+ e Docker Compose (ou PostgreSQL 16 instalado).

```bash
docker compose up -d
mvn spring-boot:run
```

Abra http://localhost:8080/. O Flyway aplica as migrações V1 a V5 automaticamente.
O projeto usa o Maven instalado (não inclui `mvnw`). As credenciais do `docker-compose.yml` são só para desenvolvimento local.

Para outro banco, configure:

| Variável | Descrição | Padrão |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/devolucao_magalu` |
| `DB_USER` | Usuário do banco | `devolucao` |
| `DB_PASSWORD` | Senha do banco | `devolucao` |
| `APP_FUSO_HORARIO` | Fuso de negócio | `America/Sao_Paulo` |
| `NOTIFICACOES_JOB_ATIVO` | Liga/desliga o job de lembretes | `true` |
| `SPRING_PROFILES_ACTIVE` | Use `prod` em produção (cookie Secure) | — |

Comandos úteis: `mvn test` (testes) e `mvn package` (gera o JAR em `target/`).

**Demonstração visual (opcional):** `python demo/preview.py` abre uma prévia com dados fictícios em http://127.0.0.1:8770/. Não é o backend real. Veja [demo/README.md](demo/README.md).

**Celular e HTTPS:** veja [USO_NO_CELULAR.md](USO_NO_CELULAR.md). Câmera e localização normalmente exigem HTTPS.

## Contas e autenticação

- Não existe senha padrão nem cadastro público. Contas são criadas por quem administra o banco.
- Cada registro em `contas_acesso` pertence a **um cliente OU um funcionário**; o perfil vem desse vínculo.
- Contas inativas são recusadas. Após **5 falhas de senha**, a conta é bloqueada por **15 minutos** (migração V5).
- Usuários, pedidos, produtos, itens, lojas e funcionários precisam existir no banco; este projeto não tem endpoints para cadastrá-los.
- Use HTTPS fora do ambiente local e não compartilhe uma conta entre vários funcionários: o login identifica o autor de cada ação na auditoria.

### Criando uma conta

1. Gere o classpath:

```bash
mvn test-compile org.apache.maven.plugins:maven-dependency-plugin:3.7.1:build-classpath -Dmdep.outputFile=target/classpath.txt
```

2. Gere o hash BCrypt (a ferramenta pede a senha sem exibi-la).

Linux/macOS:

```bash
java -cp "target/test-classes:target/classes:$(cat target/classpath.txt)" com.magalu.devolucao.tools.GerarSenhaHash
```

Windows (PowerShell):

```powershell
$cp = Get-Content target/classpath.txt -Raw
java -cp "target/test-classes;target/classes;$cp" com.magalu.devolucao.tools.GerarSenhaHash
```

3. Insira a conta no banco, com o hash gerado (nunca senha em texto puro):

```sql
-- Cliente: usuario_id precisa existir em usuarios.
INSERT INTO contas_acesso (login, senha_hash, usuario_id)
VALUES ('cliente.exemplo', '<HASH_BCRYPT_GERADO>', 1);

-- Funcionário: funcionario_id precisa existir em funcionarios.
INSERT INTO contas_acesso (login, senha_hash, funcionario_id)
VALUES ('funcionario.exemplo', '<OUTRO_HASH_BCRYPT_GERADO>', 1);
```

## API

Base: `/api/v1`. Documentação interativa em http://localhost:8080/swagger-ui.html (exige autenticação).

### Sessão e lojas

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/csrf` | Público | Token CSRF para login e escritas |
| POST | `/sessao` | Público + CSRF | Login (formulário `username` e `password`) |
| GET | `/sessao` | Autenticado | Nome, perfil e loja da conta |
| POST | `/sessao/sair` | Sessão + CSRF | Encerra a sessão |
| GET | `/lojas?busca=&pagina=0&tamanho=20` | Autenticado | Busca paginada de lojas ativas |
| GET | `/lojas/proximas?latitude=&longitude=&raioKm=` | Autenticado | Lojas ativas próximas |

### Cliente

| Método | Rota | Descrição |
|---|---|---|
| GET | `/itens?pagina=0&tamanho=20` | Seus itens comprados, paginados |
| POST | `/devolucoes` | Cria solicitação: `{"itemCompradoId":1,"lojaId":1}` |
| GET | `/devolucoes?status=&pagina=0&tamanho=20` | Lista suas solicitações |
| GET | `/devolucoes/{id}` | Detalhe, incluindo a imagem do QR |
| GET | `/devolucoes/{id}/acompanhamento` | Produto, pedido, loja, prazo e datas do fluxo |
| GET | `/devolucoes/{id}/historico` | Eventos públicos (sem dados internos de auditoria) |
| POST | `/devolucoes/{id}/qrcode/renovar` | Renova o QR vencido (se ainda válido, devolve o mesmo) |
| POST | `/devolucoes/{id}/cancelar` | Cancela antes do recebimento |
| GET | `/notificacoes?naoLidas=&pagina=0&tamanho=20` | Caixa de avisos |
| POST | `/notificacoes/{id}/ler` | Marca como lido (204) |
| GET / PUT | `/notificacoes/preferencias` | Consulta/atualiza `{"atualizacoes":true,"lembretesQr":false}` |

### Funcionário

| Método | Rota | Descrição |
|---|---|---|
| GET | `/funcionario/devolucoes?status=&destino=&minimoHoras=` | Fila da loja do funcionário, mais antigos primeiro |
| POST | `/funcionario/devolucoes/escanear` | Recebe o item: `{"token":"UUID-LIDO-NO-QR"}` |
| POST | `/funcionario/devolucoes/{id}/destino` | `{"destino":"ESTOQUE"}`, `"REPARO"` ou `"DOACAO_ONG"` |

Regras gerais:

- Todas as listas começam na página 0, com tamanho padrão 20 e máximo 100, e respondem `{ "itens": [...], "pagina": 0, "tamanho": 20, "total": N }`.
- **Identidade nunca vem do corpo da requisição.** Dono, funcionário e loja são obtidos da conta autenticada; não envie `usuarioId` nem `funcionarioId`.
- Dados de outra pessoa retornam `404`, sem revelar que existem.

Códigos de erro: `400` parâmetro/DTO inválido; `401` sem credenciais; `403` sem permissão ou CSRF; `404` inexistente; `409` loja inativa, duplicidade ou estado/prazo incompatível; `410` QR vencido, cancelado ou usado; `500` falha técnica (mensagem genérica, detalhes só no log).

### CSRF e testes manuais (Postman/curl)

O CSRF fica habilitado mesmo com HTTP Basic:

1. Configure Basic Auth com a conta desejada.
2. `GET http://localhost:8080/api/v1/csrf` e preserve o cookie `JSESSIONID`.
3. Envie o `token` retornado no header indicado por `headerName` (normalmente `X-CSRF-TOKEN`) em cada POST/PUT.
4. Inclua também Basic Auth e `Content-Type: application/json`.

Leitura simples: `curl -u cliente.exemplo http://localhost:8080/api/v1/itens` (o curl pede a senha).

## Regras de negócio

- Uma solicitação corresponde ao **item comprado inteiro**; devolução parcial não é suportada.
- Solicitação aberta, expirada ou concluída bloqueia nova solicitação do mesmo item. Depois de cancelada, uma nova é permitida se o prazo ainda valer. Um índice único parcial e bloqueio no banco protegem contra requisições simultâneas.
- Loja inativa é recusada ao criar, renovar, escanear e definir destino.
- QR substituído, usado ou cancelado não permite novo recebimento. Um QR ainda válido pode ser recebido mesmo depois do último dia de solicitação (política mantida).
- O cliente só cancela em `AGUARDANDO_ESCANEAMENTO` ou `QR_EXPIRADO`. Repetir um cancelamento é permitido e não duplica auditoria.
- **Destino definido não conclui a devolução.** O status vai para `DESTINO_PENDENTE`, sem `dataConclusao`. Nada é confirmado automaticamente.
- Na doação, é escolhida a ONG ativa compatível de menor ID (sem balanceamento de capacidade) e registrada em `ongDestinatariaId`.
- Qualquer funcionário da loja pode definir o destino; a auditoria registra quem executou.
- Busca de lojas: latitude entre -90 e 90, longitude entre -180 e 180, raio entre 0 (exclusive) e 200 km; valores inválidos retornam 400. A distância é calculada no PostgreSQL sem índice espacial (para grande volume, considere PostGIS).
- Datas usam `Clock` injetável no fuso `APP_FUSO_HORARIO`. Os campos são `TIMESTAMP` sem fuso; ao atualizar um banco existente, use o mesmo fuso dos dados anteriores. Não há conversão automática do histórico.

## Jobs em segundo plano

Ambos processam em lotes, cada lote em transação própria, com `SKIP LOCKED` para workers concorrentes e logs de quantidade, pendências e duração.

- **Expiração de QR:** marca códigos vencidos como `QR_EXPIRADO`.
- **Lembretes de QR:** avisa antes do vencimento, no máximo um lembrete por código, respeitando a preferência do cliente.

```yaml
app:
  notificacoes:
    job-ativo: true
    antecedencia-minutos: 120   # 1–1440; deve ser maior que intervalo-ms
    intervalo-ms: 300000
    tamanho-lote: 200
    max-lotes-por-execucao: 50
  qrcode:
    validade-horas: 24
    tamanho-px: 300
    job-expiracao-ms: 300000
    tamanho-lote-expiracao: 100
    max-lotes-expiracao: 50
```

Lotes: 1–1000. Intervalos: mínimo 1000 ms. Configuração incompatível impede a aplicação de iniciar. Não há lembrete retroativo se a aplicação ficar desligada até o QR vencer.

## Banco de dados

Migrações Flyway em `src/main/resources/db/migration`. **Não altere migrações já publicadas**; mudanças de schema exigem uma nova.

| Migração | Conteúdo |
|---|---|
| V1 | Schema inicial |
| V2 | Contas de acesso e índice que impede devoluções duplicadas |
| V3 | Destino pendente e ONG destinatária |
| V4 | Data de recebimento, notificações e preferências |
| V5 | Bloqueio de conta por tentativas de login falhas |

**Atenção na V2:** em banco antigo com várias solicitações não canceladas para o mesmo item, a migração falha. Nenhum dado é apagado automaticamente. Para localizar os conflitos:

```sql
SELECT item_comprado_id, COUNT(*)
FROM solicitacoes_devolucao
WHERE status <> 'CANCELADA'
GROUP BY item_comprado_id HAVING COUNT(*) > 1;
```

**Atenção na V4:** o recebimento de registros antigos é recuperado do primeiro evento `QR_ESCANEADO`. Sem essa evidência, a data fica nula (o registro vai para o fim da fila).

## Testes

```bash
mvn test
```

Os testes unitários e de API cobrem regras de negócio, identidade, perfis, BCrypt, CSRF, sessão, propriedade dos dados, renovação, cancelamento, jobs e paginação.

Os testes de integração PostgreSQL (19 cenários, em duas variantes) são **opcionais**:

- **Com Docker:** `DevolucaoPostgresTest` usa Testcontainers com `postgres:16-alpine`.
- **Sem Docker:** aponte para um banco dedicado:

  ```powershell
  $env:DEVOLUCAO_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/devolucao_teste'
  $env:DEVOLUCAO_TEST_DB_USER = 'usuario_de_teste'
  $env:DEVOLUCAO_TEST_DB_PASSWORD = 'senha_do_banco_de_teste'
  mvn test
  ```

  ⚠️ A suíte **apaga os dados das tabelas** entre os testes. Use apenas um PostgreSQL descartável, nunca produção.

Sem Docker e sem URL, esses testes são pulados: confira sempre o campo **Skipped** do Maven. O resultado da última verificação está em [VERIFICACAO.txt](VERIFICACAO.txt). O Mockito usa `mock-maker-subclass`.

## Pendências

- Integrações reais de estoque, reparo e envio para ONG ainda são `TODO`. Por isso nenhuma devolução vai a `CONCLUIDA` automaticamente.
- Seleção de ONG por capacidade ou localização.
- Notificações por e-mail, push ou SMS.
- Cadastro público de contas e devolução parcial.
- Validação em aparelhos físicos (câmera e geolocalização).

Ideias detalhadas em [MELHORIAS_FUTURAS.md](MELHORIAS_FUTURAS.md).

## Estrutura do projeto

```
src/main/java/com/magalu/devolucao/   controller, service, domain, dto, repository, config, exception
src/main/resources/static/            frontend (index.html, app.js, app.css, sw.js, fontes Poppins, jsQR)
src/main/resources/db/migration/      migrações Flyway V1–V5
src/test/java/                        testes unitários, de API e de integração
demo/                                 prévia visual opcional em Python
```

## Outros documentos

| Arquivo | Conteúdo |
|---|---|
| [INICIAR_AQUI.md](INICIAR_AQUI.md) | Guia para abrir o projeto em outro editor/assistente |
| [USO_NO_CELULAR.md](USO_NO_CELULAR.md) | Acesso por celular, HTTPS, câmera e localização |
| [NOVIDADES.md](NOVIDADES.md) | Detalhes de acompanhamento, fila e notificações |
| [ATUALIZACAO_2026-09-26.md](ATUALIZACAO_2026-09-26.md) | Correções de jobs, paginação e erros |
| [VERIFICACAO.txt](VERIFICACAO.txt) | Resultado dos testes executados |
| [AGENTS.md](AGENTS.md) / [CLAUDE.md](CLAUDE.md) | Contexto para assistentes de programação |
