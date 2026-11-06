# Correções aplicadas — 26/09/2026

Aplicadas as recomendações da revisão sobre lembretes, lotes, erros técnicos,
paginação de itens e histórico antigo. A recomendação de tornar a validação
PostgreSQL obrigatória ficou fora do escopo, conforme solicitado. Os testes
existentes continuam opcionais; não foi criado bloqueio de build ou CI.

## Mudança na resposta de GET /api/v1/itens

Antes, a rota devolvia todos os itens em uma lista direta. Agora aceita:

```text
GET /api/v1/itens?pagina=0&tamanho=20
```

Página começa em zero; tamanho padrão 20, entre 1 e 100. A resposta passa a ser:

```json
{
  "itens": [
    {
      "id": 123,
      "numeroPedido": "P123",
      "produto": "Produto de exemplo",
      "quantidade": 1,
      "valorPago": 100.00,
      "dataLimiteDevolucao": "2026-10-10",
      "elegivelParaDevolucao": true
    }
  ],
  "pagina": 0,
  "tamanho": 20,
  "total": 1
}
```

Adapte o frontend/consumidor para ler `resposta.itens` e solicitar as próximas
páginas. A ordenação é ID decrescente. Página vazia retorna lista vazia e o total.
Não existe mais consulta de todos os bloqueios do cliente: apenas os IDs da página
são verificados, em uma única consulta. Pedido e produto usam EntityGraph.

## Lembretes confiáveis dentro da capacidade configurada

- A aplicação rejeita antecedência menor ou igual ao intervalo do job. Por exemplo,
  antecedência de 1 minuto com intervalo de 5 minutos não é aceita.
- O job processa sucessivos lotes na mesma execução, sem esperar cinco minutos
  entre lotes. O padrão permite até 50 lotes de 200 candidatos, ou 10.000 candidatos.
- Cada chamada ao serviço cria uma transação nova, libera os bloqueios ao terminar
  e preserva os lotes já confirmados se um lote posterior falhar.
- SKIP LOCKED e a chave única por QR continuam protegendo o processamento concorrente.
- A contagem de inseridos reflete o INSERT real; conflito de chave ou preferência
  desativada não é contabilizado como envio.
- Logs mostram lotes, inseridos, pendentes, próximo vencimento e duração. Pendências
  remanescentes geram WARN. Esses logs podem alimentar o monitoramento operacional.

Não há garantia de aviso enquanto a aplicação estiver desligada ou se o volume
superar a capacidade. O limite de lotes impede execução infinita. Ajuste capacidade
e observe avisos antes de reduzir a antecedência. Notificações continuam internas
à API, sem integração de e-mail/push nesta entrega.

## Expiração em transações curtas

O job de expiração também processa lotes, padrão 100 registros por transação e até
50 lotes por execução. A consulta seleciona e bloqueia somente o lote, pula registros
bloqueados por outros workers e não seleciona solicitação que tenha QR ativo válido.
A regra é revalidada antes da alteração, e evento/aviso de expiração permanecem
na mesma transação. Há contagem de pendências e duração nos logs.

O agendador usa dois threads para os jobs de expiração e lembrete poderem progredir
independentemente. As transações de lote usam REQUIRES_NEW em serviços separados
dos jobs; não dependem de autochamada para criar transações.

## Configuração

```yaml
app:
  notificacoes:
    antecedencia-minutos: 120
    intervalo-ms: 300000
    tamanho-lote: 200
    max-lotes-por-execucao: 50
  qrcode:
    job-expiracao-ms: 300000
    tamanho-lote-expiracao: 100
    max-lotes-expiracao: 50
```

Lotes: 1–1000 registros. Máximo de lotes: 1–1000. Intervalos: pelo menos 1000 ms.
Antecedência de lembrete: 1–1440 minutos, estritamente maior que intervalo-ms.

## Erros e histórico

RegraNegocioException identifica conflitos esperados e retorna 409. Parâmetros
validados pela aplicação usam ParametroInvalidoException e retornam 400.
FalhaGeracaoQRCodeException e outras falhas técnicas retornam 500, com mensagem
genérica na resposta e exceção registrada no log. Erros de parsing/validação do
Spring preservam seus códigos HTTP (como 400 e 405), sem virar 500.

O histórico usa “O destino do produto foi definido.”, evitando afirmar pendência
em uma devolução antiga concluída. Novas notificações de uma devolução efetivamente
DESTINO_PENDENTE ainda informam que ela aguarda processamento. Não são alterados
estados antigos nem reescritos registros de auditoria.

## Compatibilidade e validação

Nenhuma migração foi adicionada ou modificada; o esquema permanece V1–V4.
As evoluções de integração externa e de seleção de ONG por capacidade/localização
não fazem parte destas correções e continuam dependendo de definições próprias.
Os testes PostgreSQL mantêm a execução opcional com Docker ou banco dedicado.
VERIFICACAO.txt registra os testes realmente executados nesta entrega.
