ALTER TABLE usuario
    ADD COLUMN email_original VARCHAR(150);

CREATE INDEX idx_usuario_data_exclusao
    ON usuario(data_exclusao);
