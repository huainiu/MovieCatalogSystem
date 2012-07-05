drop table DB2ADMIN.Modification if exists;

CREATE TABLE DB2ADMIN.Modification (
    IdModification INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    ModificationType INTEGER NOT NULL,
    EntityId INTEGER NOT NULL,
    Entity VARCHAR(100) NOT NULL,
    Clock Integer NOT NULL,
    Field VARCHAR(100),
    Value VARCHAR(1000),
    DbVersion INTEGER NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS DB2ADMIN.MODIFICATION_CLOCK
start with 1
INCREMENT BY 1;

DROP INDEX DB2ADMIN.IDX_MODIFICATION_ENTITYENTITYIDFIELD IF EXISTS;
CREATE INDEX DB2ADMIN.IDX_MODIFICATION_ENTITYENTITYIDFIELD on DB2ADMIN.Modification("ENTITY","ENTITYID","FIELD");

UPDATE DB2ADMIN.PARAM SET Value = '8' WHERE Name = 'VERSION';
