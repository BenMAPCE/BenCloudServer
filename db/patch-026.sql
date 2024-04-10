-- 3/27/2024
-- Correct annual metric in existing AQ surfaces

UPDATE "data".settings SET value_int=26 where "key"='version';

UPDATE data.air_quality_layer_metrics set annual_statistic_id = 1;
UPDATE data.air_quality_cell set annual_statistic_id = 1;
