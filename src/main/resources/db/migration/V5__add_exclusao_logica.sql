ALTER TABLE ambiente
    ADD COLUMN data_exclusao TIMESTAMP;

ALTER TABLE reserva
    ADD COLUMN data_exclusao TIMESTAMP;

CREATE INDEX idx_ambiente_data_exclusao
    ON ambiente(data_exclusao);

CREATE INDEX idx_reserva_data_exclusao
    ON reserva(data_exclusao);
