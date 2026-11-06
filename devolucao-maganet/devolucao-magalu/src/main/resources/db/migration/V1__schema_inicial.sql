CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    latitude_atual DOUBLE PRECISION,
    longitude_atual DOUBLE PRECISION
);

CREATE TABLE produtos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    categoria VARCHAR(100),
    elegivel_reparo BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE pedidos (
    id BIGSERIAL PRIMARY KEY,
    numero_pedido VARCHAR(50) NOT NULL UNIQUE,
    data_compra TIMESTAMP NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);

CREATE TABLE itens_comprados (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    quantidade INT NOT NULL DEFAULT 1,
    valor_pago NUMERIC(10, 2),
    data_limite_devolucao DATE
);

CREATE TABLE lojas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    endereco VARCHAR(500),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    horario_funcionamento VARCHAR(255),
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE funcionarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    matricula VARCHAR(50) NOT NULL UNIQUE,
    loja_id BIGINT NOT NULL REFERENCES lojas(id)
);

CREATE TABLE ongs_parceiras (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ong_categorias_aceitas (
    ong_id BIGINT NOT NULL REFERENCES ongs_parceiras(id),
    categoria VARCHAR(100) NOT NULL
);

CREATE TABLE solicitacoes_devolucao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    item_comprado_id BIGINT NOT NULL REFERENCES itens_comprados(id),
    loja_id BIGINT NOT NULL REFERENCES lojas(id),
    status VARCHAR(30) NOT NULL,
    destino_final VARCHAR(30),
    funcionario_responsavel_id BIGINT REFERENCES funcionarios(id),
    data_criacao TIMESTAMP NOT NULL,
    data_conclusao TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE qrcodes_devolucao (
    id BIGSERIAL PRIMARY KEY,
    token UUID NOT NULL UNIQUE,
    imagem_base64 TEXT,
    data_geracao TIMESTAMP NOT NULL,
    data_expiracao TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes_devolucao(id)
);

CREATE TABLE registros_auditoria (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes_devolucao(id),
    autor VARCHAR(255),
    acao VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    detalhes TEXT
);

-- Indices para as consultas mais frequentes
CREATE INDEX idx_qrcodes_token ON qrcodes_devolucao(token);
CREATE INDEX idx_qrcodes_expiracao ON qrcodes_devolucao(data_expiracao);
CREATE INDEX idx_solicitacoes_status ON solicitacoes_devolucao(status);
CREATE INDEX idx_itens_comprados_pedido ON itens_comprados(pedido_id);
