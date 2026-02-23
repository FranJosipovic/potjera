CREATE TABLE room_players
(
    id        VARCHAR(255) NOT NULL,
    room_id   VARCHAR(255) NOT NULL,
    player_id BIGINT       NOT NULL,
    is_host   BOOLEAN      NOT NULL,
    is_ready  BOOLEAN      NOT NULL,
    is_hunter BOOLEAN      NOT NULL,
    joined_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_room_players PRIMARY KEY (id)
);

CREATE TABLE rooms
(
    id          VARCHAR(255) NOT NULL,
    code        VARCHAR(6),
    status      VARCHAR(255),
    max_players INTEGER      NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_rooms PRIMARY KEY (id)
);

ALTER TABLE rooms
    ADD CONSTRAINT uc_rooms_code UNIQUE (code);

ALTER TABLE room_players
    ADD CONSTRAINT FK_ROOM_PLAYERS_ON_PLAYER FOREIGN KEY (player_id) REFERENCES users (id);

ALTER TABLE room_players
    ADD CONSTRAINT FK_ROOM_PLAYERS_ON_ROOM FOREIGN KEY (room_id) REFERENCES rooms (id);