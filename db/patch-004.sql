-- Removes obsolete field from air quality metrics

UPDATE "data".settings SET value_int=4 where "key"='version';

ALTER TABLE "data".air_quality_layer_metrics DROP COLUMN cell_count_above_lrl;
