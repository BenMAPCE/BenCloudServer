
UPDATE data.settings SET value_int=74 WHERE "key"='version';

ALTER TABLE "data".air_quality_layer ADD COLUMN group_name TEXT;
