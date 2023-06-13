-- Adding one more field to AQ surfaces

UPDATE "data".settings SET value_int=14 where "key"='version';

ALTER TABLE "data".air_quality_layer ADD upload_date timestamp NULL;

UPDATE "data".air_quality_layer SET upload_date=NOW();
