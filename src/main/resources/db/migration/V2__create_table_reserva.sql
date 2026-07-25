CREATE TABLE reserva (
    id BIGSERIAL PRIMARY KEY,
    ambiente_id BIGINT NOT NULL,
    solicitante VARCHAR(100) NOT NULL,
    data_reserva DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    observacao VARCHAR(500),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reserva_ambiente
        FOREIGN KEY (ambiente_id)
        REFERENCES ambiente(id)
);