ALTER TABLE usuario
    ADD COLUMN notificacoes_ativas BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE usuario
    ADD COLUMN data_exclusao TIMESTAMP;
