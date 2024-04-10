-- Add new columns for A4 updates. 
ALTER TABLE data.health_impact_function ADD COLUMN lag_fraction DOUBLE PRECISION[];
ALTER TABLE data.valuation_result ADD COLUMN result_lagged DOUBLE PRECISION[];
ALTER TABLE data.valuation_result ADD COLUMN result_lagged_disc DOUBLE PRECISION[];

ALTER TABLE data.valuation_result  ADD COLUMN result_mean_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_result  ADD COLUMN result_dev_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_result  ADD COLUMN result_variance_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_result  ADD COLUMN pct_2_5_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_result  ADD COLUMN pct_97_5_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_result  ADD COLUMN percentiles_array DOUBLE PRECISION[][];
 
ALTER TABLE data.valuation_function ADD COLUMN val_a_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_function ADD COLUMN val_b_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_function ADD COLUMN val_c_array DOUBLE PRECISION[];
ALTER TABLE data.valuation_function ADD COLUMN val_d_array DOUBLE PRECISION[];

--Update data.valuation_function. Use functions without discount rates to replace the old ones. 