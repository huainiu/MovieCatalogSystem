DROP INDEX DB2ADMIN.IDX_FILM_MEDIJLIST_NAZIVFILMA IF EXISTS;
CREATE INDEX DB2ADMIN.IDX_FILM_MEDIJLIST_NAZIVFILMA on DB2ADMIN.FILM("MEDIJ_LIST", "NAZIVFILMA");

UPDATE DB2ADMIN.PARAM SET Value = '5' WHERE Name = 'VERSION';
