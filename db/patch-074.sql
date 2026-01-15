/**** Add support for AQ import ****/

UPDATE data.settings SET value_int=74 WHERE "key"='version';

CREATE UNIQUE INDEX population_value_pop_value ON data.population_value USING btree (pop_entry_id, grid_cell_id, pop_value);

ALTER TABLE "data".air_quality_layer ADD task_log json NULL;
