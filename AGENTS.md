# Contexto para assistentes de programação

Leia INICIAR_AQUI.md, README.md, USO_NO_CELULAR.md e VERIFICACAO.txt antes de alterar o projeto. Este arquivo fornece contexto do repositório; as instruções explícitas do usuário definem o trabalho atual. Não há tarefa pendente implícita para executar ao abrir a pasta.

## Projeto
Sistema de devoluções: Java 17, Spring Boot 3.3.4, Maven, PostgreSQL 16, Flyway V1–V4. Site HTML/CSS/JavaScript nativo em src/main/resources/static, servido pelo backend na mesma origem. Não requer npm ou compilação do frontend. Poppins e jsQR são locais e incluem licenças.

Pacotes Java em src/main/java/com/magalu/devolucao: controller, service, domain, dto, repository e config. Os testes ficam em src/test/java. Consulte os arquivos existentes antes de presumir rotas ou estruturas.

## Regras que devem ser preservadas
- Identidade e loja vêm da conta autenticada, nunca de IDs enviados pelo cliente.
- Login por sessão com CSRF e cookie HttpOnly; HTTP Basic também disponível. Não desabilitar CSRF para fazer a interface funcionar.
- Definir destino mantém DESTINO_PENDENTE. As integrações externas de estoque, reparo e doação não estão implementadas; não simular sucesso ou concluir automaticamente.
- Na doação, registrar a ONG compatível selecionada. Evoluções de capacidade/localização ainda são futuras.
- Usar Clock injetado para regras temporais, respeitando o fuso configurado.
- Preservar paginação, consultas em lote e transações curtas nos jobs.
- Preservar migrações já publicadas; novas alterações de schema exigem nova migração.
- Manter o design solicitado: azul, ações verdes, Poppins, cartões compactos e navegação inferior mobile. Conferir 320/390 px e desktop ao alterar layout.
- Avisos são internos; não há push, e-mail ou SMS.

## Executar e verificar
`docker compose up -d`, depois `mvn spring-boot:run`. Abra http://localhost:8080/. Provisionamento de contas no README; não existe login padrão no backend.
`mvn test` executa testes. Integração PostgreSQL continua opcional por decisão do usuário. Testcontainers usa Docker quando disponível. A variante DEVOLUCAO_TEST_DB_URL deve apontar SOMENTE para banco descartável: a suíte trunca dados. Nunca usar banco de produção.

Relatar testes executados e pulados separadamente. O relatório entregue não prova integração em PostgreSQL nem testes em celulares físicos. Não transformar a integração em requisito obrigatório sem nova solicitação.

## Demonstração
demo/preview.py serve os mesmos arquivos visuais com uma API fictícia e dados em memória. Não é backend de produção, não implementa suas garantias e não deve ser publicado como o sistema real. Não copiar autenticação ou regras simplificadas da demonstração para o código Java.

Não incluir senhas reais, .env, target, ambientes locais ou dados pessoais no versionamento/pacote.
