-- Adding more fields to AQ surfaces

UPDATE "data".settings SET value_int=13 where "key"='version';

ALTER TABLE "data".air_quality_layer ADD aq_year text;
ALTER TABLE "data".air_quality_layer ADD description text;
ALTER TABLE "data".air_quality_layer ADD source text;
ALTER TABLE "data".air_quality_layer ADD data_type text;
ALTER TABLE "data".air_quality_layer ADD filename text;
