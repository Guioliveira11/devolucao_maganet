# Abrir em outro editor ou assistente

1. Extraia o ZIP inteiro.
2. Abra a pasta **devolucao-magalu**, que contém pom.xml, como pasta de trabalho no Antigravity, Claude Code ou outro editor. Não abra apenas o HTML.
3. Peça ao assistente para ler AGENTS.md e README.md. CLAUDE.md também aponta para o contexto comum. Se o aplicativo não carregar esses arquivos automaticamente, envie o texto abaixo na conversa.

## Mensagem inicial pronta para copiar

> Estou continuando este projeto de devoluções. Leia AGENTS.md, README.md, USO_NO_CELULAR.md e VERIFICACAO.txt. Preserve o backend Spring Boot e o site responsivo existente, com cabeçalho azul, Poppins e botões verdes. Diferencie a demonstração Python do sistema real. Antes de alterar algo, explique o estado atual e aguarde minha próxima tarefa. Não torne os testes PostgreSQL obrigatórios e não marque destinos pendentes como concluídos.

## Ver o design rapidamente

Requer Python 3.9+ instalado. Na pasta extraída:

```sh
python demo/preview.py
```

No Windows também pode usar `py -3 demo/preview.py`; em Linux/macOS, `python3 demo/preview.py`.
Abra http://127.0.0.1:8770/ no mesmo computador.
Cliente: login `cliente`, senha `teste`. Funcionário: login `funcionario`, senha `teste`.
Encerre com Ctrl+C. A demonstração usa somente a biblioteca padrão do Python e inclui a imagem QR já gerada e validada. Seus dados são fictícios e reiniciam a cada execução; o QR é fixo e compartilhado entre registros fictícios. Ela não testa validade, autorização, persistência ou integrações do backend.

## Executar o sistema completo

Requisitos: JDK 17, Maven 3.9+ e PostgreSQL 16 (ou Docker Compose).

```sh
docker compose up -d
mvn spring-boot:run
```

Abra http://localhost:8080/. Crie as contas e os dados necessários conforme README.md; os logins da demonstração não existem automaticamente no banco real. Maven baixará dependências no primeiro uso; não há dependência do cache ou dos caminhos da máquina original.

Comandos: `mvn test` para testes; `mvn package` para gerar o JAR em target. Para acessar de celulares, consulte USO_NO_CELULAR.md, incluindo HTTPS para câmera/localização.

Não é necessário instalar Python para executar o sistema Java; Python serve apenas à demonstração opcional. Não há configuração de conta, chave de API ou extensão específica exigida pelo projeto para o editor escolhido.

## Conteúdo e limites

Inclui código-fonte completo, frontend, fontes/licenças, migrações, testes, Compose e documentação. Não inclui JDK, Maven, PostgreSQL, dependências baixadas, credenciais reais ou banco populado. As credenciais do Compose são exclusivamente de desenvolvimento.
O projeto não foi aberto/testado dentro dos aplicativos Antigravity ou Claude Code nesta entrega; os arquivos de contexto e comandos são independentes do editor.
