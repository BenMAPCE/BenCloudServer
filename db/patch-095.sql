/**** Add archived column to incidence datasets and air quality layers ****/

UPDATE data.settings SET value_int=95 WHERE "key"='version';

ALTER TABLE "data".incidence_dataset ADD archived smallint NULL DEFAULT 0;
ALTER TABLE "data".air_quality_layer ADD archived smallint NULL DEFAULT 0;
