-- 9/27/2023
-- Revise list of AQ surfaces
-- Add additional exposure function group and rename existing
-- Delete all existing task templates, tasks, and results

UPDATE "data".settings SET value_int=18 where "key"='version';

DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=7; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=7; DELETE FROM data.air_quality_layer WHERE id=7; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=35; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=35; DELETE FROM data.air_quality_layer WHERE id=35; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=15; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=15; DELETE FROM data.air_quality_layer WHERE id=15; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=20; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=20; DELETE FROM data.air_quality_layer WHERE id=20; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=17; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=17; DELETE FROM data.air_quality_layer WHERE id=17; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=18; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=18; DELETE FROM data.air_quality_layer WHERE id=18; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=24; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=24; DELETE FROM data.air_quality_layer WHERE id=24; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=22; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=22; DELETE FROM data.air_quality_layer WHERE id=22; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=37; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=37; DELETE FROM data.air_quality_layer WHERE id=37; 
DELETE FROM data.air_quality_cell WHERE air_quality_layer_id=38; DELETE FROM data.air_quality_layer_metrics WHERE air_quality_layer_id=38; DELETE FROM data.air_quality_layer WHERE id=38; 
UPDATE data.air_quality_layer SET name='Ozone Baseline Example 2023', description='Annual data for baseline ozone 8 hour max concentrations in 2023', source='US EPA', filename=NULL WHERE id=6;
UPDATE data.air_quality_layer SET name='Ozone Policy Example 2023', description='Policy scenario for annual baseline ozone 8 hour max concentrations in 2023', source='US EPA', filename=NULL WHERE id=36;
UPDATE data.air_quality_layer SET name='PM Baseline Example 2032', description='Baseline air quality surface for projected annual 24-hour average PM2.5 concentrations in 2032', source='US EPA', filename=NULL WHERE id=16;
UPDATE data.air_quality_layer SET name='PM Policy Example 1 2032', description='Least stringent policy scenario for projected annual 24-hour average PM2.5 concentrations in 2032', source='US EPA', filename=NULL WHERE id=19;
UPDATE data.air_quality_layer SET name='PM Policy Example 3 2032', description='Most stringent policy scenario for projected annual 24-hour average PM2.5 concentrations in 2032', source='US EPA', filename=NULL WHERE id=23;
UPDATE data.air_quality_layer SET name='PM Policy Example 2 2032', description='Middle stringency policy scenario for projected annual 24-hour average PM2.5 concentrations in 2032', source='US EPA', filename=NULL WHERE id=21;

-- Revise name of existing exposure group
UPDATE "data".exposure_function_group
	SET help_text='Exposure configurations most commonly used by EPA',"name"='EPA Default Subpopulations - 2022 Rulemaking'
	WHERE id=1;

-- Add in functions for the variables that are useful and don't already exist
INSERT INTO "data".exposure_function (id,exposure_dataset_id,population_group,race_id,ethnicity_id,gender_id,start_age,end_age,variable_id,function_text) VALUES
	 (17,1,'Blue Collar Workers (0-99)',5,3,3,0,99,84,'DELTA * POPULATION * VARIABLE'),
	 (18,1,'No Health Insurance (1-17)',5,3,3,1,17,85,'DELTA * POPULATION * VARIABLE'),
	 (19,1,'No Health Insurance (18-39)',5,3,3,18,39,86,'DELTA * POPULATION * VARIABLE'),
	 (20,1,'No Health Insurance (40-64)',5,3,3,40,64,87,'DELTA * POPULATION * VARIABLE'),
	 (21,1,'No Health Insurance (<65)',5,3,3,0,65,88,'DELTA * POPULATION * VARIABLE'),
	 (22,1,'Below 2x Poverty Line (0-99)',5,3,3,0,99,91,'DELTA * POPULATION * VARIABLE'),
	 (23,1,'Above 2x Poverty Line (0-99)',5,3,3,0,99,92,'DELTA * POPULATION * VARIABLE'),
	 (24,1,'Speaks English Less Than Very Well (0-99)',5,3,3,0,99,99,'DELTA * POPULATION * VARIABLE'),
	 (25,1,'Speaks English Less Than Well (0-99)',5,3,3,0,99,101,'DELTA * POPULATION * VARIABLE'),
	 (26,1,'Speaks English Very Well or Better (0-99)',5,3,3,0,99,103,'DELTA * POPULATION * VARIABLE');

INSERT INTO "data".exposure_function (id,exposure_dataset_id,population_group,race_id,ethnicity_id,gender_id,start_age,end_age,variable_id,function_text) VALUES
	 (27,1,'Speaks English Well or Better (0-99)',5,3,3,0,99,105,'DELTA * POPULATION * VARIABLE');
	 	 
INSERT INTO "data".exposure_function_group (id,"name",help_text, pollutant_id) VALUES
	 (2,'All available options','Includes all population stratifications and available variables', 6);

INSERT INTO "data".exposure_function_group_member (exposure_function_group_id,exposure_function_id) VALUES
	 (2,1),
	 (2,2),
	 (2,3),
	 (2,4),
	 (2,5),
	 (2,6),
	 (2,7),
	 (2,8),
	 (2,9),
	 (2,10);
INSERT INTO "data".exposure_function_group_member (exposure_function_group_id,exposure_function_id) VALUES
	 (2,11),
	 (2,12),
	 (2,13),
	 (2,14),
	 (2,15),
	 (2,16),
	 (2,17),
	 (2,18),
	 (2,19),
	 (2,20),
	 (2,21),
	 (2,22),
	 (2,23),
	 (2,24),
	 (2,25),
	 (2,26),
	 (2,27);
	 
-- Clean up ALL old templates, tasks, and results
truncate data.task_config;
truncate data.task_worker;
truncate data.task_queue;
truncate data.task_complete;
truncate data.task_batch;
truncate data.hif_result;
truncate data.hif_result_function_config;
truncate data.hif_result_dataset;
truncate data.valuation_result_dataset ;
truncate data.valuation_result_function_config ;
truncate data.valuation_result ;
truncate data.exposure_result;
truncate data.exposure_result_function_config;
truncate data.exposure_result_dataset;

-- Reset ALL sequences
SELECT SETVAL('data.age_range_id_seq', COALESCE(MAX(id), 1) ) FROM data.age_range;
SELECT SETVAL('data.air_quality_layer_id_seq', COALESCE(MAX(id), 1) ) FROM data.air_quality_layer;
SELECT SETVAL('data.air_quality_layer_metrics_id_seq', COALESCE(MAX(id), 1) ) FROM data.air_quality_layer_metrics;
SELECT SETVAL('data.crosswalk_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.crosswalk_dataset;
SELECT SETVAL('data.endpoint_group_id_seq', COALESCE(MAX(id), 1) ) FROM data.endpoint_group;
SELECT SETVAL('data.endpoint_id_seq', COALESCE(MAX(id), 1) ) FROM data.endpoint;
SELECT SETVAL('data.ethnicity_id_seq', COALESCE(MAX(id), 1) ) FROM data.ethnicity;
SELECT SETVAL('data.exposure_function_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.exposure_function_dataset;
SELECT SETVAL('data.exposure_function_group_id_seq', COALESCE(MAX(id), 1) ) FROM data.exposure_function_group;
SELECT SETVAL('data.exposure_function_id_seq', COALESCE(MAX(id), 1) ) FROM data.exposure_function;
SELECT SETVAL('data.exposure_result_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.exposure_result_dataset;
SELECT SETVAL('data.gender_id_seq', COALESCE(MAX(id), 1) ) FROM data.gender;
SELECT SETVAL('data.grid_definition_id_seq', COALESCE(MAX(id), 1) ) FROM data.grid_definition;
SELECT SETVAL('data.health_impact_function_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.health_impact_function_dataset;
SELECT SETVAL('data.health_impact_function_group_id_seq', COALESCE(MAX(id), 1) ) FROM data.health_impact_function_group;
SELECT SETVAL('data.health_impact_function_id_seq', COALESCE(MAX(id), 1) ) FROM data.health_impact_function;
SELECT SETVAL('data.hif_result_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.hif_result_dataset;
SELECT SETVAL('data.incidence_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.incidence_dataset;
SELECT SETVAL('data.incidence_entry_id_seq', COALESCE(MAX(id), 1) ) FROM data.incidence_entry;
SELECT SETVAL('data.income_growth_adj_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.income_growth_adj_dataset;
SELECT SETVAL('data.income_growth_adj_factor_id_seq', COALESCE(MAX(id), 1) ) FROM data.income_growth_adj_factor;
SELECT SETVAL('data.inflation_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.inflation_dataset;
SELECT SETVAL('data.pollutant_id_seq', COALESCE(MAX(id), 1) ) FROM data.pollutant;
SELECT SETVAL('data.pollutant_metric_id_seq', COALESCE(MAX(id), 1) ) FROM data.pollutant_metric;
SELECT SETVAL('data.pop_config_id_seq', COALESCE(MAX(id), 1) ) FROM data.pop_config;
SELECT SETVAL('data.population_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.population_dataset;
SELECT SETVAL('data.population_entry_id_seq', COALESCE(MAX(id), 1) ) FROM data.population_entry;
SELECT SETVAL('data.race_id_seq', COALESCE(MAX(id), 1) ) FROM data.race;
SELECT SETVAL('data.seasonal_metric_id_seq', COALESCE(MAX(id), 1) ) FROM data.seasonal_metric;
SELECT SETVAL('data.seasonal_metric_season_id_seq', COALESCE(MAX(id), 1) ) FROM data.seasonal_metric_season;
SELECT SETVAL('data.statistic_type_id_seq', COALESCE(MAX(id), 1) ) FROM data.statistic_type;
SELECT SETVAL('data.task_batch_id_seq', COALESCE(MAX(id), 1) ) FROM data.task_batch;
SELECT SETVAL('data.task_complete_task_id_seq', COALESCE(MAX(task_id), 1) ) FROM data.task_complete;
SELECT SETVAL('data.task_config_id_seq', COALESCE(MAX(id), 1) ) FROM data.task_config;
SELECT SETVAL('data.task_queue_task_id_seq', COALESCE(MAX(task_id), 1) ) FROM data.task_queue;
SELECT SETVAL('data.task_worker_task_id_seq', COALESCE(MAX(task_id), 1) ) FROM data.task_worker;
SELECT SETVAL('data.valuation_function_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.valuation_function_dataset;
SELECT SETVAL('data.valuation_function_id_seq', COALESCE(MAX(id), 1) ) FROM data.valuation_function;
SELECT SETVAL('data.valuation_result_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.valuation_result_dataset;
SELECT SETVAL('data.variable_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.variable_dataset;
SELECT SETVAL('data.variable_entry_id_seq', COALESCE(MAX(id), 1) ) FROM data.variable_entry;

GRANT CONNECT ON DATABASE benmap TO benmap_system;
GRANT USAGE ON SCHEMA data, grids, tiger, tiger_data, topology TO benmap_system;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA data, grids TO benmap_system;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA data, grids TO benmap_system;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA data, grids TO benmap_system;

vacuum analyze;
