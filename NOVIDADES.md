# Acompanhamento, fila por loja e notificações

Atualizado em 26/09/2026. Funcionalidades de backend; ainda não há telas.
As rotas existentes foram preservadas e os novos endpoints seguem HTTP Basic.
POST e PUT exigem cookie da sessão e token CSRF obtido em GET /api/v1/csrf.

## 1. Acompanhamento pelo cliente

| Método | Rota | Uso |
|---|---|---|
| GET | /api/v1/devolucoes | Lista somente as solicitações do cliente autenticado |
| GET | /api/v1/devolucoes/{id}/acompanhamento | Produto, pedido, loja, endereço, prazo e datas do fluxo |
| GET | /api/v1/devolucoes/{id}/historico | Eventos públicos, sem autor ou detalhes internos da auditoria |

Lista: `?status=DESTINO_PENDENTE&pagina=0&tamanho=20`. Status é opcional.
Histórico: `?pagina=0&tamanho=20`.
Todas as páginas começam em zero; tamanho padrão 20 e máximo 100.
Formato estável: `{ "itens": [...], "pagina": 0, "tamanho": 20, "total": 3 }`.
Lista e histórico mostram os mais recentes primeiro, com ID como desempate.
Detalhe e histórico de outra pessoa retornam 404, sem revelar sua existência.
Nenhuma rota aceita um usuarioId para escolher o dono dos dados.

O acompanhamento contém `id`, `numeroPedido`, `produto`, `quantidade`, `lojaId`,
`loja`, `enderecoLoja`, `prazoSolicitacao`, `status`, `destino`, `ongDestinatariaId`,
`criadaEm`, `recebidaEm`, `destinoDefinidoEm`, `concluidaEm` e `qrExpiraEm`.
O prazo de solicitação e a validade do QR são conceitos diferentes. A imagem
do QR continua disponível em GET /api/v1/devolucoes/{id}; ela não é carregada
em cada linha da listagem. O status QR_EXPIRADO é atualizado pelo job existente;
entre vencimento e execução do job, `qrExpiraEm` pode ser nulo enquanto o status
persistido ainda é AGUARDANDO_ESCANEAMENTO.

O histórico reutiliza apenas ações conhecidas da auditoria, com textos públicos
fixos. Informações internas, matrículas e textos livres não são retornados.
Dados antigos aparecem quando existe auditoria; não é criado histórico fictício.

## 2. Fila de trabalho por loja

GET /api/v1/funcionario/devolucoes

Exemplo:
`?status=DESTINO_PENDENTE&destino=ESTOQUE&minimoHoras=24&pagina=0&tamanho=20`

- Apenas funcionários; a loja é obtida da conta autenticada, nunca da requisição.
- Loja inativa não acessa a fila.
- Sem filtros, inclui EM_ANALISE e DESTINO_PENDENTE. Outros status são rejeitados.
- `destino`: ESTOQUE, REPARO ou DOACAO_ONG, opcional.
- `minimoHoras`: tempo mínimo desde o recebimento, opcional, entre 0 e 876000.
- Mais antigos por recebimento aparecem primeiro. O ID desempata.
- Retorna pedido, produto, quantidade, status, destino, ONG, `recebidaEm` e
  `minutosDesdeRecebimento`; não expõe CPF, e-mail ou dados de outros clientes.
- V4 recupera o recebimento antigo a partir do primeiro evento QR_ESCANEADO.
  Sem essa evidência, a data fica nula, aparece no fim da fila e não corresponde
  ao filtro de tempo mínimo. Não se usa criação da solicitação como recebimento.

## 3. Notificações dentro da API

| Método | Rota | Uso |
|---|---|---|
| GET | /api/v1/notificacoes | Caixa paginada do cliente autenticado |
| POST | /api/v1/notificacoes/{id}/ler | Marca como lida; repetir preserva o primeiro horário |
| GET | /api/v1/notificacoes/preferencias | Consulta preferências |
| PUT | /api/v1/notificacoes/preferencias | Atualiza as duas preferências |

Lista: `?naoLidas=true&pagina=0&tamanho=20`; sem filtro, mostra todas.
Resposta inclui `id`, `solicitacaoId`, `tipo`, `mensagem`, `criadaEm`, `lidaEm`
e `qrExpiraEm`. O total da página filtrada por não lidas permite exibir um contador.
Marcar como lida retorna 204; notificação inexistente ou alheia retorna 404.

Exemplo de corpo das preferências:

```json
{"atualizacoes":true,"lembretesQr":false}
```

As duas propriedades são obrigatórias. Por padrão, ambas estão habilitadas para
a caixa interna. Desativar não apaga o histórico nem notificações já recebidas.
Não há reenvio retroativo de atualizações ao reativar. Lembretes voltam a ser
considerados para códigos ainda válidos dentro da janela.

Atualizações cobrem criação, renovação, cancelamento, recebimento, definição de
destino e expiração pelo job. Destino definido informa explicitamente que o
processamento está pendente. Não existe aviso de conclusão fictícia: a futura
integração confiável terá de adicionar seu evento confirmado ao fluxo.

As notificações e a ação de negócio são gravadas na mesma transação. Uma falha
desfaz ambas. A chave única por evento de auditoria impede duplicidade do mesmo
evento; repetir cancelamento já realizado não cria outro evento.

### Lembretes de QR

O job verifica lotes de até 200 candidatos, com até 50 lotes consecutivos por
execução (padrão). O intervalo entre execuções é cinco minutos, com antecedência
padrão de duas horas. Cada lote confirma sua própria transação antes do seguinte.
Só considera QR ativo, ainda válido, com
solicitação aguardando escaneamento e preferência de lembrete habilitada.
Revalida sob bloqueio da solicitação, usa SKIP LOCKED para workers concorrentes
e chave única `qr:<id>` para limitar a um lembrete por código. Renovar cria outro
código e permite outro lembrete. Não envia lembrete para QR usado/cancelado/vencido.

Uma notificação anterior não é apagada quando o código vence ou é renovado;
`qrExpiraEm` registra o prazo daquele lembrete. Consulte o acompanhamento para
ver o estado atual. Se a aplicação ficar desligada até o código vencer, não há
lembrete retroativo. Em grandes volumes, dimensione intervalo e processamento.
A antecedência deve ser estritamente maior que o intervalo do job; uma configuração
incompatível impede a inicialização da aplicação. Os logs mostram quantidade
inserida, pendências, próximo vencimento e duração, com aviso quando sobra fila.
O limite de lotes evita que uma execução se prolongue indefinidamente. Indisponibilidade
ou carga acima da capacidade ainda exigem acompanhamento operacional.

Configuração em application.yml:

```yaml
app:
  notificacoes:
    job-ativo: true
    antecedencia-minutos: 120 # 1–1440; deve superar o intervalo abaixo
    intervalo-ms: 300000
    tamanho-lote: 200
    max-lotes-por-execucao: 50
```

`NOTIFICACOES_JOB_ATIVO=false` desativa apenas o job de lembretes.
O fuso de todas as datas segue `APP_FUSO_HORARIO`, padrão America/Sao_Paulo.
E-mail, push e notificações do sistema operacional não foram integrados.

## Banco e testes

V4 adiciona data de recebimento, preferências, notificações e índices. Não altera
o significado das solicitações antigas já concluídas. O Flyway aplica V1–V4.

Com Docker ativo:

```bash
mvn test
```

Como alternativa, use um PostgreSQL 16 **descartável e exclusivo para testes**.
A suíte trunca as tabelas entre testes; nunca use banco de produção ou com dados
que queira preservar. Exemplo PowerShell, para um banco de testes já criado:

```powershell
$env:DEVOLUCAO_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/devolucao_teste'
$env:DEVOLUCAO_TEST_DB_USER = 'usuario_de_teste'
$env:DEVOLUCAO_TEST_DB_PASSWORD = 'senha_do_banco_de_teste'
mvn test
```

Ao configurar a URL, a suíte local executa os mesmos testes de integração e a
variante Docker fica desativada. Sem URL e sem Docker, ambas são puladas.
Cada variante possui 19 cenários. A ausência de erros em testes unitários não
substitui a execução real desses cenários. Consulte VERIFICACAO.txt para os
resultados efetivamente obtidos na preparação deste ZIP.


## Paginação dos itens comprados e tratamento de erros

GET /api/v1/itens?pagina=0&tamanho=20 retorna o mesmo envelope de paginação das
outras listas. Os itens são ordenados por ID decrescente, pertencem somente ao
cliente autenticado e a elegibilidade usa apenas os bloqueios da página consultada.
Essa rota retornava uma lista direta; atualize o consumidor para ler `resposta.itens`.
Os detalhes estão em ATUALIZACAO_2026-09-26.md.

Falhas técnicas inesperadas retornam HTTP 500, com mensagem pública genérica e
detalhes nos logs internos. Conflitos de negócio explícitos continuam HTTP 409;
parâmetros inválidos continuam HTTP 400. A mensagem histórica DESTINO_DEFINIDO
agora informa somente que o destino foi definido, sem afirmar que continua pendente.
