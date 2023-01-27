-- liquibase formatted sql

--changeset artemiev:1
CREATE TABLE client
(
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(100),
    volunteer         BOOLEAN,
    volunteer_active  BOOLEAN,
    administrator     BOOLEAN,
    chat_id           BIGINT NOT NULL UNIQUE,
    last_visit        TIMESTAMPTZ,
    time_zone         VARCHAR(3)
);

CREATE TABLE contact
(
    id         SERIAL PRIMARY KEY,
    type       INTEGER NOT NULL,
    value      VARCHAR(100),
    client_id    INTEGER REFERENCES client(id)
);

CREATE TABLE support
(
    id                   SERIAL PRIMARY KEY,
    client_id_client     INTEGER REFERENCES client(id) NOT NULL,
    client_id_volunteer  INTEGER REFERENCES client(id),
    type                 INTEGER NOT NULL,
    datetime_begin       TIMESTAMPTZ NOT NULL ,
    datetime_finish      TIMESTAMPTZ,
    finish               BOOLEAN
);

--changeset yakovlev:1
CREATE TABLE probation
(
    id          SERIAL PRIMARY KEY,
    client_id   INTEGER REFERENCES client(id),
    pet_id      INTEGER REFERENCES pet(id),
    date_begin  TIMESTAMPTZ,
    date_finish TIMESTAMPTZ,
    success     BOOLEAN,
    result      TEXT
);

CREATE TABLE probation_journal
(
    id              SERIAL,
    probation_id    INTEGER REFERENCES probation(id),
    date            TIMESTAMPTZ,
    photo_received  BOOLEAN,
    report_received BOOLEAN
);

CREATE TABLE probation_data
(
    id                   SERIAL PRIMARY KEY,
    probation_journal_id INTEGER REFERENCES probation_journal(id),
    type                 INTEGER,
    link                 TEXT
);

--changeset sirko:1
CREATE TABLE pet
(
    id       SERIAL PRIMARY KEY,
    nickname VARCHAR(100),
    breed VARCHAR(300),
    age INTEGER,
    character VARCHAR(300),
    looking_for_owner BOOLEAN
);

CREATE TABLE photo_pets
(
    id SERIAL PRIMARY KEY,
    pet_id INTEGER REFERENCES pet(id),
    photo TEXT
);


