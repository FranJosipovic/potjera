CREATE TABLE game_sessions
(
    id          VARCHAR(255) NOT NULL,
    room_id     VARCHAR(255) NOT NULL,
    game_stage  VARCHAR(255),
    started_at  TIMESTAMP WITHOUT TIME ZONE,
    finished_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_game_sessions PRIMARY KEY (id)
);

ALTER TABLE game_sessions
    ADD CONSTRAINT uc_game_sessions_room UNIQUE (room_id);

ALTER TABLE game_sessions
    ADD CONSTRAINT FK_GAME_SESSIONS_ON_ROOM FOREIGN KEY (room_id) REFERENCES rooms (id);