# Sistema de devolução de itens — Magalu

Para abrir em outro editor ou assistente, comece por [INICIAR_AQUI.md](INICIAR_AQUI.md).

Site responsivo para clientes e funcionários, com backend Java 17, Spring Boot 3.3.4 e PostgreSQL 16. Abra `/` no navegador após iniciar o servidor. Consulte [USO_NO_CELULAR.md](USO_NO_CELULAR.md).

## Executar

Pré-requisitos: JDK 17, Maven 3.9+ e Docker Compose (ou PostgreSQL 16 instalado).

```bash
docker compose up -d
mvn spring-boot:run
```

O Flyway aplica automaticamente as migrações V1 a V7. O projeto usa Maven instalado; não inclui `mvnw`.
As credenciais de banco do Compose são para desenvolvimento local.
Para outro banco, configure `SPRING_DATASOURCE_URL`, `DB_USER` e `DB_PASSWORD`.

V2 cria as contas de acesso e um índice que bloqueia devoluções duplicadas.
Se houver várias solicitações não canceladas para o mesmo item em um banco antigo,
a migração falhará: revise esses registros antes de aplicá-la. Nenhum histórico é
apagado automaticamente. Consulta para localizar conflitos:

```sql
SELECT item_comprado_id, COUNT(*)
FROM solicitacoes_devolucao
WHERE status <> 'CANCELADA'
GROUP BY item_comprado_id HAVING COUNT(*) > 1;
```

## Contas e autenticação

O site usa login por sessão e cookie HttpOnly. HTTP Basic continua disponível para clientes da API. A página de entrada, seus arquivos estáticos e a obtenção do token CSRF são públicos; os dados exigem autenticação. Use HTTPS fora do ambiente local.
Não existe senha padrão nem cadastro público de contas.
Cada registro em `contas_acesso` está vinculado a **um cliente OU um funcionário**.
O perfil é derivado desse vínculo; a API não aceita um perfil escolhido pelo cliente.
A senha é armazenada como BCrypt e contas inativas são recusadas. A recuperação de senha atende clientes e funcionários por e-mail; funcionários precisam ter o campo `funcionarios.email` preenchido.

Os registros de usuários, pedidos, produtos, itens, lojas e funcionários devem existir
no banco. Os endpoints de cadastro desses dados não fazem parte deste projeto.
Para criar uma conta, gere o hash localmente:

```bash
mvn test-compile org.apache.maven.plugins:maven-dependency-plugin:3.7.1:build-classpath -Dmdep.outputFile=target/classpath.txt
```

Em um terminal Linux/macOS:

```bash
java -cp "target/test-classes:target/classes:$(cat target/classpath.txt)" com.magalu.devolucao.tools.GerarSenhaHash
```

Em um terminal PowerShell no Windows:

```powershell
$cp = Get-Content target/classpath.txt -Raw
java -cp "target/test-classes;target/classes;$cp" com.magalu.devolucao.tools.GerarSenhaHash
```

A ferramenta pede a senha sem exibi-la e imprime seu hash. Substitua os valores abaixo
pelos dados corretos e pelo hash gerado (não guarde senha em texto puro no SQL):

```sql
-- Conta de cliente: usuario_id precisa existir em usuarios.
INSERT INTO contas_acesso (login, senha_hash, usuario_id)
VALUES ('cliente.exemplo', '<HASH_BCRYPT_GERADO>', 1);

-- Conta de funcionário: funcionario_id precisa existir em funcionarios.
INSERT INTO contas_acesso (login, senha_hash, funcionario_id)
VALUES ('funcionario.exemplo', '<OUTRO_HASH_BCRYPT_GERADO>', 1);

-- Cadastre o e-mail do funcionário para habilitar a recuperação de senha.
UPDATE funcionarios SET email = 'funcionario@empresa.com' WHERE id = 1;
```

Faça o provisionamento por acesso administrativo ao banco. Não distribua uma conta
única entre vários funcionários: o login identifica o autor de cada ação.

## Recuperação de senha por e-mail

A tela de login oferece o link **Esqueceu sua senha?** para clientes e funcionários. O link enviado por e-mail expira em 15 minutos, é de uso único e o token é armazenado no banco somente como hash SHA-256. A senha nova é validada com no mínimo 8 caracteres e armazenada com BCrypt.

Configure o servidor SMTP antes de usar a funcionalidade em ambiente real. Exemplo de variáveis de ambiente (não coloque credenciais diretamente no repositório):

```bash
APP_PUBLIC_URL=https://seu-dominio.com
MAIL_HOST=smtp.seuprovedor.com
MAIL_PORT=587
MAIL_USERNAME=nao-responda@seu-dominio.com
MAIL_PASSWORD=sua-senha-de-aplicativo
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Em desenvolvimento, `docker compose up -d` também inicia o Mailpit: a aplicação envia por `localhost:1025` e você pode visualizar os e-mails em `http://localhost:8025`. A migração V7 adiciona `funcionarios.email` sem preencher registros antigos; atualize os e-mails existentes antes de esperar que esses funcionários consigam recuperar a senha. A resposta da solicitação é genérica, para não revelar se um endereço está cadastrado.

## Endpoints

| Método | Rota | Perfil | Corpo / comportamento |
|---|---|---|---|
| GET | `/api/v1/csrf` | Público | Retorna o token CSRF para login e escritas |
| POST | `/api/v1/sessao` | Público, com CSRF | Formulário `username` e `password`; cria sessão |
| POST | `/api/v1/senha/recuperar` | Público, com CSRF | `{"email":"pessoa@exemplo.com"}`; envia link se houver conta ativa |
| POST | `/api/v1/senha/validar` | Público, com CSRF | `{"token":"..."}`; informa se o link ainda é válido |
| POST | `/api/v1/senha/redefinir` | Público, com CSRF | `{"token":"...","novaSenha":"..."}`; altera a senha |
| GET | `/api/v1/sessao` | Autenticado | Nome, perfil e loja da conta |
| POST | `/api/v1/sessao/sair` | Sessão, com CSRF | Encerra sessão |
| GET | `/api/v1/lojas?busca=&pagina=0&tamanho=20` | Autenticado | Busca paginada de lojas ativas por nome ou endereço |
| GET | `/api/v1/itens` | Cliente | Lista paginada dos seus itens; `pagina=0&tamanho=20` (máximo 100) |
| GET | `/api/v1/lojas/proximas?latitude=0&longitude=0` | Autenticado | Lista lojas ativas próximas |
| POST | `/api/v1/devolucoes` | Cliente | `{"itemCompradoId":1,"lojaId":1}` |
| GET | `/api/v1/devolucoes/{id}` | Cliente dono | Consulta sua solicitação |
| POST | `/api/v1/devolucoes/{id}/qrcode/renovar` | Cliente dono | Sem corpo; renova código vencido |
| POST | `/api/v1/devolucoes/{id}/cancelar` | Cliente dono | Sem corpo; cancela antes do recebimento |
| POST | `/api/v1/funcionario/devolucoes/escanear` | Funcionário da loja | `{"token":"UUID-LIDO-NO-QR"}` |
| POST | `/api/v1/funcionario/devolucoes/{id}/destino` | Funcionário da loja | `{"destino":"ESTOQUE"}`, `REPARO` ou `DOACAO_ONG` |

**Mudança nos corpos das requisições:** não envie mais `usuarioId` ou `funcionarioId`.
Esses campos foram removidos dos DTOs. Campos extras enviados por clientes antigos
são ignorados pela configuração padrão do Jackson e jamais definem a identidade.
O dono é obtido pela conta autenticada; o funcionário e sua loja também.
Qualquer funcionário autenticado da loja escolhida pode definir o destino, mas a
auditoria registra quem efetivamente executou essa ação.

### CSRF e chamadas de teste

A proteção CSRF permanece habilitada, inclusive com HTTP Basic. No Postman:

1. Configure Basic Auth com a conta correta.
2. Faça `GET http://localhost:8080/api/v1/csrf`.
3. Preserve o cookie `JSESSIONID` retornado (Postman gerencia cookies).
4. Copie `token` da resposta e envie-o no header indicado por `headerName`
   (normalmente `X-CSRF-TOKEN`) em cada POST da mesma sessão.
5. Envie também Basic Auth e `Content-Type: application/json` nas chamadas com JSON.

Exemplo de leitura via terminal (o curl pedirá a senha):

```bash
curl -u cliente.exemplo http://localhost:8080/api/v1/itens
```

Swagger: `http://localhost:8080/swagger-ui.html`, também exige autenticação.
Para testar POST pela interface Swagger, será necessário fornecer o token CSRF;
o fluxo Postman acima é o caminho documentado para os testes manuais.

## Regras corrigidas

- Um cliente só cria, consulta, renova ou cancela suas próprias devoluções.
- Loja inativa é recusada ao criar, renovar, escanear e definir destino.
- Renovação só ocorre em `AGUARDANDO_ESCANEAMENTO` ou `QR_EXPIRADO`, dentro do prazo
  do item. Funciona mesmo antes de o job atualizar o status para `QR_EXPIRADO`.
- Se o QR ainda está válido, renovar devolve o mesmo código, sem gerar outro.
- Um QR substituído, usado ou cancelado não permite novo recebimento.
- O cliente cancela apenas antes do recebimento (`AGUARDANDO_ESCANEAMENTO` ou
  `QR_EXPIRADO`). Repetir um cancelamento já realizado é permitido e não duplica
  auditoria. Após `EM_ANALISE` ou `CONCLUIDA`, o cliente não pode cancelar.
- Uma solicitação corresponde ao **item comprado inteiro, com toda a quantidade**.
  Devolução parcial não está modelada nesta versão.
- Solicitação aberta, expirada ou concluída bloqueia uma nova para o mesmo item.
  Depois de cancelada, uma nova é permitida se o prazo ainda estiver válido.
- Criação usa bloqueio do item no banco e índice único parcial para proteger contra
  requisições simultâneas. Outras alterações bloqueiam a solicitação enquanto a
  transação está em andamento.
- O job de expiração processa somente códigos vencidos ainda ativos e revalida a
  solicitação sob bloqueio; um código antigo não invalida um código novo.
- Respostas são montadas dentro da transação para carregar os dados necessários.
- Os campos de texto longo do QR e da auditoria foram alinhados com `TEXT` no SQL.

Respostas comuns: `401` sem credenciais válidas; `403` sem permissão ou CSRF;
`404` para registro inexistente; `409` para loja inativa, duplicidade ou estado/prazo
incompatível; `410` para QR vencido, cancelado ou já utilizado; `400` para DTO inválido.

## Testes

```bash
mvn test
```

Os testes unitários e de API verificam regras, identidade, perfil, senha BCrypt,
CSRF, propriedade dos dados, renovação e cancelamento.
`DevolucaoPostgresTest` usa Testcontainers e PostgreSQL 16 para validar migrações,
persistência, concorrência, acompanhamento, fila e notificações. Exige Docker ativo
e acesso à imagem `postgres:16-alpine`; sem Docker, esses testes são pulados.
Há também execução opcional em PostgreSQL descartável dedicado, documentada em
NOVIDADES.md. Verifique sempre o campo **Skipped** do resultado do Maven.

A configuração Mockito usa `mock-maker-subclass`, pois os testes não precisam de
mocks de classes finais nem de anexar um agente à JVM.

## Escopo ainda pendente

As integrações reais de estoque, ordem de reparo e envio para ONG continuam como
`TODO`. Não foram implementadas nesta correção. Também não há frontend, cadastro
público de contas, devolução parcial nem notificações por e-mail/push.


## Atualização de 19/09/2026

- Definir destino agora muda de `EM_ANALISE` para `DESTINO_PENDENTE`, sem preencher
  `dataConclusao`. Não há confirmação automática nem endpoint público para fingir
  a execução de estoque, reparo ou coleta. Uma futura integração confiável deverá
  confirmar a operação antes da conclusão. O estado pendente é persistido na própria
  solicitação; não existe ainda worker de entrega, fila ou retentativa automática.
- Doação escolhe a ONG ativa compatível de menor ID (critério determinístico,
  sem balanceamento de capacidade) e persiste `ong_destinataria_id`.
  A resposta inclui `ongDestinatariaId` e `dataDefinicaoDestino`.
- V3 adiciona os campos e índices. Solicitações historicamente `CONCLUIDA` permanecem
  intactas; a migração não afirma que as integrações antigas foram executadas.
- A listagem de itens usa paginação, com pedido/produto carregados por EntityGraph.
  Os bloqueios são consultados em conjunto apenas para os IDs da página retornada.
  A resposta agora é um objeto com `itens`, `pagina`, `tamanho` e `total`.
  Veja ATUALIZACAO_2026-09-26.md para adaptar consumidores da API.
- A busca de lojas valida valores finitos, latitude [-90,90], longitude [-180,180]
  e raio 0 < raioKm <= 200. Valores inválidos retornam 400. PostgreSQL filtra as lojas
  ativas usando distância esférica; Java ordena apenas os resultados filtrados.
  A consulta ainda calcula distâncias no banco sem índice espacial. Para grande
  volume, considerar PostGIS e índice geográfico.
- Clock injetável, com `APP_FUSO_HORARIO=America/Sao_Paulo` por padrão. As entidades
  recebem a data/hora explicitamente. Os campos existentes continuam TIMESTAMP sem
  fuso; configure o mesmo fuso de negócio usado pelos dados anteriores antes de
  atualizar um banco existente. Não foi feita conversão silenciosa do histórico.
- O prazo do item continua validado ao criar/renovar. Um QR ainda válido pode ser
  recebido depois do último dia de solicitação: essa política anterior foi mantida.

### Verificação

Execute `mvn test` com Java 17 e Maven, e Docker ativo para os testes PostgreSQL.
Os testes novos incluem destino pendente, ONG, geolocalização, validação HTTP,
consulta conjunta de bloqueios e vencimentos com relógio fixo.
Consulte VERIFICACAO.txt para o resultado efetivamente obtido nesta atualização.


## Acompanhamento, fila e notificações

Veja [NOVIDADES.md](NOVIDADES.md) para os endpoints e exemplos da nova versão.
O backend agora oferece acompanhamento paginado pelo cliente, histórico público,
fila por loja com idade desde recebimento e caixa de notificações com preferências
e lembretes de QR. Não inclui interface visual, e-mail nem push.
A migração V4 cria os dados necessários; preserve V1–V3 já aplicadas.


## Correções de 26/09/2026

Veja [ATUALIZACAO_2026-09-26.md](ATUALIZACAO_2026-09-26.md).
Inclui validação da janela dos lembretes, jobs em lotes com transações separadas,
logs de pendências, erros técnicos com HTTP 500, paginação de itens e histórico
de destino neutro. As migrações V1–V4 não foram alteradas.
Os testes PostgreSQL continuam opcionais; não foi adicionada exigência ao build.
