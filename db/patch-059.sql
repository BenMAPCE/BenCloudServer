/**** Add created_date to crosswalk_dataset table ****/

UPDATE "data".settings SET value_int=59 WHERE "key"='version';

ALTER TABLE "data".crosswalk_dataset ADD created_date timestamp null;
UPDATE "data".crosswalk_dataset SET created_date = NOW();
