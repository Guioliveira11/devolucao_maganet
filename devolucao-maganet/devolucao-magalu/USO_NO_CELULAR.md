# Site responsivo

O projeto inclui um site servido pelo próprio Spring Boot. O cliente abre a URL no navegador do celular; Java e PostgreSQL rodam no servidor. Não é um APK e não funciona abrindo o HTML diretamente ou sem conexão.

## Iniciar e acessar

1. Instale Java 17 e Maven 3.9+ no servidor/computador. Prepare PostgreSQL 16 conforme README.md; para desenvolvimento, use `docker compose up -d`.
2. Execute `mvn spring-boot:run` na pasta do projeto.
3. No computador, abra `http://localhost:8080/`.
4. Para testar em um celular na mesma rede, use `http://IP-DO-COMPUTADOR:8080/`. O celular precisa conseguir alcançar essa porta na rede. `localhost` no celular aponta para o próprio celular, não para o computador.
5. Use contas previamente provisionadas conforme README.md. Não há senha padrão, cadastro público ou dados de demonstração na entrega.

Para disponibilização aos usuários, hospede o backend e o banco e configure um domínio com HTTPS válido. Ative `SPRING_PROFILES_ACTIVE=prod`: esse perfil exige cookie de sessão Secure. Ele não cria certificado nem configura HTTPS sozinho. Se houver proxy reverso, configure-o para encaminhar ao backend apenas por uma rede confiável. Não exponha PostgreSQL à internet. Configure as credenciais reais pelas variáveis documentadas no README.

## Funcionalidades

- Cliente: compras paginadas, solicitação e seleção de loja por busca ou localização, QR de entrega, acompanhamento e histórico, renovação do QR vencido e cancelamento antes do recebimento.
- Avisos internos: atualizações, lembretes do QR, marcação de leitura e preferências. São exibidos dentro do site; não são push, SMS ou e-mail.
- Funcionário: fila da própria loja, filtros de etapa/destino/tempo, leitura de QR pela câmera ou código digitado e definição de destino com confirmação.
- Login por sessão, encerramento de sessão, CSRF nas escritas, cookie HttpOnly/SameSite e recursos locais sem CDN. A aplicação não grava senhas em localStorage.

A interface se adapta ao celular e ao desktop, com navegação inferior no celular, campos e botões para toque e suporte à redução de animações.

O design segue o moodboard fornecido: cabeçalho azul, ações verdes, cartões brancos compactos, detalhes roxos e tipografia Poppins. As fontes Regular e Bold são distribuídas localmente, com licença SIL Open Font License em `static/fonts/OFL.txt`, obtidas do repositório oficial https://github.com/google/fonts/tree/main/ofl/poppins. O verde das ações foi escurecido para melhorar a leitura do texto branco.

## Câmera e localização

Em celulares, câmera e localização normalmente exigem HTTPS e a permissão do usuário. Em HTTP pela rede local, use a busca de lojas por nome/endereço e o campo de código. O leitor reconhece o UUID contido no QR, pede confirmação do recebimento e encerra a câmera ao sair da tela. Os frames são processados no navegador e não enviados ao servidor; apenas o token lido é enviado ao confirmar.

O decodificador local é jsQR 1.4.0, obtido do pacote oficial npm `jsqr`. Sua licença MIT está em `src/main/resources/static/vendor/jsQR-LICENSE.txt`. Fonte: https://github.com/cozmo/jsQR. O arquivo distribuído é `dist/jsQR.js` do pacote.

## Verificações e limites

Consulte VERIFICACAO.txt para os resultados. A prévia de interface foi verificada no navegador com respostas fictícias da API e larguras de 320, 390 e 1280 pixels. Isso não substitui um teste completo com o backend e PostgreSQL, nem a validação em aparelhos Android/iPhone reais. Câmera física e geolocalização não foram acionadas nesta verificação.

As integrações externas de estoque, reparo e doação continuam pendentes. Definir destino mantém a solicitação em DESTINO_PENDENTE. Os testes de integração PostgreSQL continuam opcionais, conforme solicitado; não houve nova migração de banco nesta atualização.
