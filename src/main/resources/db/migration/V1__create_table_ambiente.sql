CREATE TABLE ambiente (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    capacidade INTEGER NOT NULL,
    localizacao VARCHAR(100),
    descricao VARCHAR(255),
    possui_projetor BOOLEAN DEFAULT FALSE,
    possui_tv BOOLEAN DEFAULT FALSE,
    possui_microfone BOOLEAN DEFAULT FALSE,
    possui_computadores BOOLEAN DEFAULT FALSE,
    ativo BOOLEAN DEFAULT TRUE
);