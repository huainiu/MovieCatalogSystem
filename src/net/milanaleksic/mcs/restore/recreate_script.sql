create schema if not exists DB2ADMIN;

set schema DB2ADMIN;

drop table param if exists;

drop table logerakcija if exists;

drop table loger if exists;

drop table zauzima if exists;

drop table film if exists;

drop table medij if exists;

drop table tipmedija if exists;

drop table zanr if exists;

drop table pozicija if exists;

CREATE TABLE Param (
    IdParam INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Name varchar(100) NOT NULL,
    Value varchar(100) NOT NULL
);

CREATE TABLE Loger(
    IdLoger INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    IdLogerAkcija int NOT NULL,
    Kontekst varchar(100) NOT NULL,
    Vreme TIMESTAMP NOT NULL
);

CREATE TABLE LogerAkcija (
    IdLogerAkcija INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Naziv varchar(100) NOT NULL
);

CREATE TABLE Film(
    IdFilm INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    NazivFilma varchar(100) NOT NULL,
    PrevodNazivaFilma varchar(100) NOT NULL,
    Godina int NOT NULL,
    IdZanr int NOT NULL,
    Komentar varchar(1000),
    IMDBRejting numeric(3, 1) NOT NULL
);

CREATE TABLE Medij(
    IdMedij INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Indeks int NOT NULL,
    idTip int NOT NULL,
    IdPozicija int NOT NULL
);

CREATE TABLE Pozicija(
    IdPozicija INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Pozicija varchar(100) NOT NULL
);

CREATE TABLE TipMedija(
    IdTip INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Naziv varchar(100) NOT NULL
);

CREATE TABLE Zanr(
    IdZanr INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    Zanr varchar(100) NOT NULL
);

CREATE TABLE Zauzima(
    idMedij int NOT NULL,
    idFilm int NOT NULL
);

ALTER TABLE Film  ADD  FOREIGN KEY(IdZanr)
REFERENCES Zanr (IdZanr);

ALTER TABLE Medij  ADD  FOREIGN KEY(IdPozicija)
REFERENCES Pozicija (IdPozicija);

ALTER TABLE Medij  ADD  FOREIGN KEY(idTip)
REFERENCES TipMedija (IdTip);

ALTER TABLE Zauzima  ADD  FOREIGN KEY(idFilm)
REFERENCES Film (IdFilm)
ON DELETE CASCADE;

ALTER TABLE Zauzima  ADD  FOREIGN KEY(idMedij)
REFERENCES Medij (IdMedij)
ON DELETE CASCADE;

ALTER TABLE Loger  ADD  FOREIGN KEY(idLogerAkcija)
REFERENCES LogerAkcija (IdLogerAkcija)
ON DELETE CASCADE;

DROP INDEX DB2ADMIN.IDX_PARAM_NAME IF EXISTS;
CREATE UNIQUE INDEX IDX_PARAM_NAME on DB2ADMIN.PARAM("NAME");

DROP INDEX DB2ADMIN.IDX_FILM_NAZIVFILMA IF EXISTS;
CREATE INDEX IDX_FILM_NAZIVFILMA on DB2ADMIN.FILM("NAZIVFILMA");

DROP INDEX DB2ADMIN.IDX_FILM_PREVODNAZIVAFILMA IF EXISTS;
CREATE INDEX IDX_FILM_PREVODNAZIVAFILMA on DB2ADMIN.FILM("PREVODNAZIVAFILMA");

DROP INDEX DB2ADMIN.IDX_FILM_IDZANR IF EXISTS;
CREATE INDEX IDX_FILM_IDZANR on DB2ADMIN.FILM("IDZANR");

DROP INDEX DB2ADMIN.IDX_MEDIJ_INDEKS IF EXISTS;
CREATE INDEX IDX_MEDIJ_INDEKS on DB2ADMIN.MEDIJ("INDEKS");

DROP INDEX DB2ADMIN.IDX_MEDIJ_IDPOZICIJA IF EXISTS;
CREATE INDEX IDX_MEDIJ_IDPOZICIJA on DB2ADMIN.MEDIJ("IDPOZICIJA");

DROP INDEX DB2ADMIN.IDX_ZAUZIMA_IDMEDIJ IF EXISTS;
CREATE INDEX IDX_ZAUZIMA_IDMEDIJ on DB2ADMIN.ZAUZIMA("IDMEDIJ");

DROP INDEX DB2ADMIN.IDX_ZAUZIMA_IDFILM IF EXISTS;
CREATE INDEX IDX_ZAUZIMA_IDFILM on DB2ADMIN.ZAUZIMA("IDFILM");

INSERT INTO PARAM(Name, Value)
VALUES('VERSION', '1');
