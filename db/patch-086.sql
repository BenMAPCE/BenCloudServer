/*BWD-248 Store group name in air-quality-layer  */

UPDATE "data".settings SET value_int=86 where "key"='version';

ALTER TABLE "data".air_quality_layer ADD group_name text NULL;
