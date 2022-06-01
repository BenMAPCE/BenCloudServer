CREATE INDEX crosswalk_entry_crosswalk_id_idx ON data.crosswalk_entry USING btree (crosswalk_id, source_grid_cell_id, target_grid_cell_id, percentage);
CREATE INDEX incidence_value_incidence_entry_id_idx ON data.incidence_value USING btree (incidence_entry_id, grid_cell_id, value);
CREATE INDEX hif_result_hif_result_dataset_id_idx ON data.hif_result USING btree (hif_result_dataset_id, hif_id, grid_col, grid_row, grid_cell_id, result, population, baseline_aq, scenario_aq, delta_aq, incidence, result_mean, baseline, standard_dev, result_variance, pct_2_5, pct_97_5);
CREATE INDEX valuation_result_valuation_result_dataset_id_idx ON "data".valuation_result (valuation_result_dataset_id,vf_id,hif_id,grid_col,grid_row,grid_cell_id,population,"result",result_mean,standard_dev,result_variance,pct_2_5,pct_97_5);
CREATE INDEX population_entry_id_idx ON "data".population_entry (id,pop_dataset_id,race_id,ethnicity_id,gender_id,age_range_id,pop_year);
