# Funcionalidades propostas

Estas sugestões foram elaboradas a partir do fluxo e do código deste projeto.
Os itens 1 e 2 foram implementados no backend. O item 4 foi implementado como
caixa interna e lembretes de QR; e-mail/push e confirmação real de conclusão
continuam dependentes de integrações. Consulte NOVIDADES.md. Os demais itens
permanecem propostas.

## 1. Acompanhamento de devoluções pelo cliente — prioridade alta
Adicionar GET /api/v1/devolucoes com paginação e filtros, restrito ao cliente
autenticado. Mostrar loja, produto, prazo, estado atual e histórico de eventos
adequado ao cliente, sem expor campos internos da auditoria.
Benefício: o cliente consegue retomar uma solicitação sem guardar seu ID.
Aceite: nenhum cliente consegue listar solicitações de outra conta.

## 2. Fila de trabalho por loja — prioridade alta
Listar solicitações em análise e destinos pendentes, com filtro por idade e destino.
Restringir sempre à loja do funcionário autenticado. Registrar tempo de recebimento
para identificar atrasos; não usar apenas a data de criação pelo cliente.
Aceite: funcionários de uma loja não acessam a fila de outra.

## 3. Integrações com confirmação e retentativa — prioridade alta
Após definir o contrato real com estoque/assistência/coleta, registrar eventos
transacionais em uma outbox e processá-los com identificador idempotente, retentativas
e rastreamento de falha. Persistir o protocolo externo e concluir apenas após uma
confirmação confiável. Autenticar callbacks e rejeitar confirmações duplicadas ou
incompatíveis. Exige definição das APIs externas; não basta criar um botão Concluir.
Aceite: indisponibilidade externa não perde operações nem duplica entrada em estoque.

## 4. Notificações e lembrete de QR — prioridade média
Notificar recebimento, atualização de destino e conclusão; oferecer aviso de QR
próximo do vencimento. Respeitar preferências do cliente e evitar mensagens
duplicadas usando eventos e chaves idempotentes.
Aceite: nenhuma notificação de conclusão enquanto o destino estiver pendente.

## 5. Motivo da devolução e inspeção — prioridade média
Capturar motivo estruturado e observações. Permitir ao funcionário registrar
condição do item e justificativa do destino. Fotos exigem limite de tamanho,
validação do formato, controle de acesso e política de retenção.
Aceite: a decisão de encaminhamento é rastreável e anexos não são públicos.

## 6. Reembolso acompanhado separadamente — depende do escopo comercial
Modelar valor, meio de pagamento, protocolo e estados do reembolso separadamente
da destinação física do produto. Definir primeiro a integração com pagamentos e
as regras comerciais; não concluir que doação ou estoque significa dinheiro devolvido.
Aceite: falha ou repetição da integração não gera pagamento duplicado.

## 7. Devolução parcial — após revisão do modelo
Permitir devolver parte da quantidade comprada. Substituir a restrição atual de
uma solicitação não cancelada por item por controle transacional do saldo de
quantidades e valores. Exige migração, tratamento de cancelamento e concorrência.
Aceite: duas solicitações simultâneas nunca excedem a quantidade comprada.

Ordem sugerida: acompanhamento e fila por loja; integração confiável;
notificações e inspeção; reembolso e devolução parcial conforme o produto evoluir.
