-- Adding help_text to variables
-- Adding new fields to incidence dataset and entry
-- Adding exposure function tables

UPDATE "data".settings SET value_int=15 where "key"='version';

ALTER TABLE "data".variable_entry ADD help_text text NULL;

ALTER TABLE "data".incidence_dataset ADD user_id text NULL;
ALTER TABLE "data".incidence_dataset ADD share_scope smallint NULL DEFAULT 0;
ALTER TABLE "data".incidence_dataset ADD filename text;
ALTER TABLE "data".incidence_dataset ADD upload_date timestamp NULL;

UPDATE "data".incidence_dataset set share_scope = 1;
UPDATE "data".incidence_dataset SET upload_date=NOW();

ALTER TABLE "data".incidence_entry ADD timeframe text NULL;
ALTER TABLE "data".incidence_entry ADD units text NULL;
ALTER TABLE "data".incidence_entry ADD distribution text NULL;
ALTER TABLE "data".incidence_entry ADD standard_error float8 NULL;


CREATE TABLE "data".exposure_function_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT exposure_function_dataset_pkey PRIMARY KEY (id)
);

INSERT INTO "data".exposure_function_dataset ("name") VALUES('EPA Standard Exposure Functions');

CREATE TABLE "data".exposure_function (
	id serial4 NOT NULL,
	exposure_dataset_id int4 NULL,
	population_group text NULL,
	race_id int4 NULL,
	ethnicity_id int4 NULL,
	gender_id int4 NULL,
	start_age int4 NULL,
	end_age int4 NULL,
	variable_id int4 NULL,
	function_text text NULL,
	CONSTRAINT exposure_function_pkey PRIMARY KEY (id)
);

INSERT INTO "data".exposure_function (id,exposure_dataset_id,population_group,race_id,ethnicity_id,gender_id,start_age,end_age,variable_id,function_text) VALUES
	 (1,1,'All: Reference (0-99)',5,3,3,0,99,NULL,'DELTA * POPULATION'),
	 (2,1,'Asian (0-99)',1,3,3,0,99,NULL,'DELTA * POPULATION'),
	 (3,1,'Black (0-99)',2,3,3,0,99,NULL,'DELTA * POPULATION'),
	 (4,1,'Native American (0-99)',3,3,3,0,99,NULL,'DELTA * POPULATION'),
	 (5,1,'White (0-99)',4,3,3,0,99,NULL,'DELTA * POPULATION'),
	 (6,1,'Non-Hispanic (0-99)',5,1,3,0,99,NULL,'DELTA * POPULATION'),
	 (7,1,'Hispanic (0-99)',5,2,3,0,99,NULL,'DELTA * POPULATION'),
	 (8,1,'Female (0-99)',5,3,1,0,99,NULL,'DELTA * POPULATION'),
	 (9,1,'Male (0-99)',5,3,2,0,99,NULL,'DELTA * POPULATION'),
	 (10,1,'Children (0-17)',5,3,3,0,17,NULL,'DELTA * POPULATION');
INSERT INTO "data".exposure_function (id,exposure_dataset_id,population_group,race_id,ethnicity_id,gender_id,start_age,end_age,variable_id,function_text) VALUES
	 (11,1,'Adults (18-64)',5,3,3,18,64,NULL,'DELTA * POPULATION'),
	 (12,1,'Older Adults (64-99)',5,3,3,64,99,NULL,'DELTA * POPULATION'),
	 (13,1,'Below Poverty Line (0-99)',5,3,3,0,99,89,'DELTA * POPULATION * VARIABLE'),
	 (14,1,'Above Poverty Line (0-99)',5,3,3,0,99,90,'DELTA * POPULATION * VARIABLE'),
	 (15,1,'Less educated (>24; no high school)',5,3,3,25,99,93,'DELTA * POPULATION * VARIABLE'),
	 (16,1,'More educated (>24; high school or more)',5,3,3,25,99,94,'DELTA * POPULATION * VARIABLE');

	 
-- exposure_function_group
CREATE TABLE "data".exposure_function_group (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	help_text text NULL,
	pollutant_id int4 NULL,
	CONSTRAINT exposure_function_group_pkey PRIMARY KEY (id)
);

INSERT INTO "data".exposure_function_group (id,"name",help_text, pollutant_id) VALUES
	 (1,'NAAQS PM RIA 2022','Exposure configurations used to generate the 2022 NAAQS PM RIA', 6);

-- exposure_function_group_member
CREATE TABLE "data".exposure_function_group_member (
	exposure_function_group_id int4 NULL,
	exposure_function_id int4 NULL
);

INSERT INTO "data".exposure_function_group_member (exposure_function_group_id,exposure_function_id) VALUES
	 (1,1),
	 (1,2),
	 (1,3),
	 (1,4),
	 (1,5),
	 (1,6),
	 (1,7),
	 (1,8),
	 (1,9),
	 (1,10);
INSERT INTO "data".exposure_function_group_member (exposure_function_group_id,exposure_function_id) VALUES
	 (1,11),
	 (1,12),
	 (1,13),
	 (1,14),
	 (1,15),
	 (1,16);

-- exposure_result_dataset
CREATE TABLE "data".exposure_result_dataset (
	id serial4 NOT NULL,
	task_uuid text NULL,
	"name" text NULL,
	population_dataset_id int4 NULL,
	population_year int4 NULL,
	baseline_aq_layer_id int4 NULL,
	scenario_aq_layer_id int4 NULL,
	task_log json NULL,
	user_id text NULL,
	sharing_scope int2 NULL DEFAULT 0,
	grid_definition_id int4 NULL,
	CONSTRAINT exposure_result_dataset_pkey PRIMARY KEY (id)
);

-- exposure_result_function_config
CREATE TABLE "data".exposure_result_function_config (
	exposure_result_dataset_id int4 NULL,
	exposure_function_id int4 NULL,
	start_age int4 NULL,
	end_age int4 NULL,
	variable_id int4 NULL,
	race_id int4 NULL,
	gender_id int4 NULL,
	ethnicity_id int4 NULL,
	metric_id int4 NULL,
	seasonal_metric_id int4 NULL,
	metric_statistic int4 NULL,
	exposure_function_instance_id int4 NULL
);

-- exposure_result
CREATE TABLE "data".exposure_result (
	exposure_result_dataset_id int4 NULL,
	exposure_function_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	subgroup_population float8 NULL,
	all_population float8 NULL,
	delta_aq float8 NULL,
	baseline_aq float8 NULL,
	scenario_aq float8 NULL,
	exposure_function_instance_id int4 NULL
);

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