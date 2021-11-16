ALTER TABLE "data".hif_result ADD baseline_aq float8 NULL;
ALTER TABLE "data".hif_result ADD scenario_aq float8 NULL;
ALTER TABLE "data".hif_result RENAME COLUMN delta_aq TO delta_aqx;
ALTER TABLE "data".hif_result_function_config ADD metric_id int4 NULL;
ALTER TABLE "data".hif_result_function_config ADD seasonal_metric_id int4 NULL;
ALTER TABLE "data".hif_result_function_config ADD metric_statistic int4 NULL;