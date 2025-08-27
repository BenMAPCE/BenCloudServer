
UPDATE data.settings SET value_int=70 WHERE "key"='version';

CREATE INDEX idx_pv_grid_entry ON data.population_value(pop_entry_id, grid_cell_id);

CREATE INDEX idx_pgw_combo ON data.population_growth_weight(target_grid_cell_id, race_id, ethnicity_id, source_grid_cell_id, base_pop_year, pop_dataset_id);

vacuum analyze;
