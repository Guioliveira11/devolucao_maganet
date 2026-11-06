# Pacote corrigido — Maga Next

Este pacote parte do ZIP enviado na conversa e corrige as incompatibilidades de compilação identificadas no log do Render:

- métodos ausentes nos repositories;
- relacionamento ContaAcesso -> OngParceira;
- método IdentidadeService.ong() e validação de sessão com três tipos de conta;

## Antes de publicar

1. Faça backup do banco PostgreSQL.
3. Se a versão 7 registrada no banco for outra, NÃO publique a V8 antes de reconciliar o histórico com cuidado. Não apague linhas de `flyway_schema_history` nem execute `flyway repair` sem backup e análise.
4. Na pasta `devolucao-magalu`, execute `mvn clean package` localmente.
5. Só depois faça commit e push da branch de trabalho e acompanhe os logs do Render.

## Observação de validação

O ambiente de geração deste pacote não possui Maven instalado; portanto, as alterações foram feitas com base nos fontes e erros fornecidos, mas a compilação Maven e os testes automatizados não puderam ser executados aqui. Trate o build local como etapa obrigatória antes do deploy.
