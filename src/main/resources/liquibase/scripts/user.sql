-- liquibase formatted sql

--changeset artemiev:1


--changeset yakovlev:1
CREATE TABLE probation
(
    id         SERIAL,
    userId     INTEGER,
    petId      INTEGER,
    dateBegin  TIMESTAMPTZ,
    dateFinish TIMESTAMPTZ,
    success    BOOLEAN,
    result     TEXT
);

CREATE TABLE probationJournal
(
    id             SERIAL,
    probationId    INTEGER,
    date           TIMESTAMPTZ,
    photoReceived  BOOLEAN,
    reportReceived BOOLEAN
);

CREATE TABLE probationData
(
    id                 SERIAL,
    probationJournalId INTEGER,
    type               INTEGER,
    link               TEXT
);

--changeset sirko:1