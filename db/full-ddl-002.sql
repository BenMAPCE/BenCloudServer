-- DROP SCHEMA "data";

CREATE SCHEMA "data" AUTHORIZATION benmap_system;

-- DROP SEQUENCE "data".age_range_id_seq;

CREATE SEQUENCE "data".age_range_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".air_quality_layer_id_seq;

CREATE SEQUENCE "data".air_quality_layer_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".air_quality_layer_metrics_id_seq;

CREATE SEQUENCE "data".air_quality_layer_metrics_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".crosswalk_dataset_id_seq;

CREATE SEQUENCE "data".crosswalk_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".endpoint_group_id_seq;

CREATE SEQUENCE "data".endpoint_group_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".endpoint_id_seq;

CREATE SEQUENCE "data".endpoint_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".ethnicity_id_seq;

CREATE SEQUENCE "data".ethnicity_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".gender_id_seq;

CREATE SEQUENCE "data".gender_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".grid_definition_id_seq;

CREATE SEQUENCE "data".grid_definition_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".health_impact_function_dataset_id_seq;

CREATE SEQUENCE "data".health_impact_function_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".health_impact_function_group_id_seq;

CREATE SEQUENCE "data".health_impact_function_group_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".health_impact_function_id_seq;

CREATE SEQUENCE "data".health_impact_function_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".hif_result_dataset_id_seq;

CREATE SEQUENCE "data".hif_result_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".incidence_dataset_id_seq;

CREATE SEQUENCE "data".incidence_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".incidence_entry_id_seq;

CREATE SEQUENCE "data".incidence_entry_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".income_growth_adj_dataset_id_seq;

CREATE SEQUENCE "data".income_growth_adj_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".income_growth_adj_factor_id_seq;

CREATE SEQUENCE "data".income_growth_adj_factor_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".inflation_dataset_id_seq;

CREATE SEQUENCE "data".inflation_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".pollutant_id_seq;

CREATE SEQUENCE "data".pollutant_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".pollutant_metric_id_seq;

CREATE SEQUENCE "data".pollutant_metric_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".pop_config_id_seq;

CREATE SEQUENCE "data".pop_config_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".population_dataset_id_seq;

CREATE SEQUENCE "data".population_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".population_entry_id_seq;

CREATE SEQUENCE "data".population_entry_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".race_id_seq;

CREATE SEQUENCE "data".race_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".seasonal_metric_id_seq;

CREATE SEQUENCE "data".seasonal_metric_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".seasonal_metric_season_id_seq;

CREATE SEQUENCE "data".seasonal_metric_season_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".statistic_type_id_seq;

CREATE SEQUENCE "data".statistic_type_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".task_complete_task_id_seq;

CREATE SEQUENCE "data".task_complete_task_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".task_config_id_seq;

CREATE SEQUENCE "data".task_config_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".task_queue_task_id_seq;

CREATE SEQUENCE "data".task_queue_task_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".task_worker_task_id_seq;

CREATE SEQUENCE "data".task_worker_task_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".valuation_function_dataset_id_seq;

CREATE SEQUENCE "data".valuation_function_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".valuation_function_id_seq;

CREATE SEQUENCE "data".valuation_function_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".valuation_result_dataset_id_seq;

CREATE SEQUENCE "data".valuation_result_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".variable_dataset_id_seq;

CREATE SEQUENCE "data".variable_dataset_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "data".variable_entry_id_seq;

CREATE SEQUENCE "data".variable_entry_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;-- "data".air_quality_layer_metrics definition

-- Drop table

-- DROP TABLE "data".air_quality_layer_metrics;

CREATE TABLE "data".air_quality_layer_metrics (
	id serial4 NOT NULL,
	air_quality_layer_id int4 NULL,
	metric_id int4 NULL,
	seasonal_metric_id int4 NULL,
	annual_statistic_id int4 NULL,
	cell_count int4 NULL,
	min_value float8 NULL,
	max_value float8 NULL,
	mean_value float8 NULL,
	pct_2_5 float8 NULL,
	pct_97_5 float8 NULL,
	cell_count_above_lrl int4 NULL,
	CONSTRAINT air_quality_layer_metrics_pkey PRIMARY KEY (id)
);


-- "data".crosswalk_dataset definition

-- Drop table

-- DROP TABLE "data".crosswalk_dataset;

CREATE TABLE "data".crosswalk_dataset (
	id serial4 NOT NULL,
	source_grid_id int4 NULL,
	target_grid_id int4 NULL,
	CONSTRAINT crosswalk_dataset_pkey PRIMARY KEY (id)
);


-- "data".crosswalk_entry definition

-- Drop table

-- DROP TABLE "data".crosswalk_entry;

CREATE TABLE "data".crosswalk_entry (
	crosswalk_id int4 NULL,
	source_col int4 NULL,
	source_row int4 NULL,
	source_grid_cell_id int8 NULL,
	target_col int4 NULL,
	target_row int4 NULL,
	target_grid_cell_id int8 NULL,
	percentage float8 NULL
);
CREATE INDEX crosswalk_entry_crosswalk_id_idx ON data.crosswalk_entry USING btree (crosswalk_id, source_grid_cell_id, target_grid_cell_id, percentage);


-- "data".endpoint_group definition

-- Drop table

-- DROP TABLE "data".endpoint_group;

CREATE TABLE "data".endpoint_group (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT endpoint_group_pkey PRIMARY KEY (id)
);


-- "data".ethnicity definition

-- Drop table

-- DROP TABLE "data".ethnicity;

CREATE TABLE "data".ethnicity (
	id serial4 NOT NULL,
	"name" text NULL,
	CONSTRAINT ethnicity_pkey PRIMARY KEY (id)
);


-- "data".gender definition

-- Drop table

-- DROP TABLE "data".gender;

CREATE TABLE "data".gender (
	id serial4 NOT NULL,
	"name" text NULL,
	CONSTRAINT gender_pkey PRIMARY KEY (id)
);


-- "data".grid_definition definition

-- Drop table

-- DROP TABLE "data".grid_definition;

CREATE TABLE "data".grid_definition (
	id serial4 NOT NULL,
	"name" text NULL,
	col_count int4 NULL,
	row_count int4 NULL,
	is_admin_layer varchar(1) NULL,
	draw_priority int4 NULL,
	outline_color varchar(50) NULL,
	table_name text NULL,
	CONSTRAINT grid_definition_pkey PRIMARY KEY (id)
);


-- "data".health_impact_function definition

-- Drop table

-- DROP TABLE "data".health_impact_function;

CREATE TABLE "data".health_impact_function (
	id serial4 NOT NULL,
	health_impact_function_dataset_id int4 NULL,
	endpoint_group_id int4 NULL,
	endpoint_id int4 NULL,
	pollutant_id int4 NULL,
	metric_id int4 NULL,
	seasonal_metric_id int4 NULL,
	metric_statistic int4 NULL,
	author text NULL,
	function_year int4 NULL,
	"location" text NULL,
	other_pollutants text NULL,
	qualifier text NULL,
	reference text NULL,
	start_age int4 NULL,
	end_age int4 NULL,
	function_text text NULL,
	incidence_dataset_id int4 NULL,
	prevalence_dataset_id int4 NULL,
	variable_dataset_id int4 NULL,
	beta float8 NULL,
	dist_beta text NULL,
	p1_beta float8 NULL,
	p2_beta float8 NULL,
	val_a float8 NULL,
	name_a text NULL,
	val_b float8 NULL,
	name_b text NULL,
	val_c float8 NULL,
	name_c text NULL,
	baseline_function_text text NULL,
	race_id int4 NULL,
	gender_id int4 NULL,
	ethnicity_id int4 NULL,
	start_day int4 NULL,
	end_day int4 NULL,
	CONSTRAINT health_impact_function_pkey PRIMARY KEY (id)
);


-- "data".health_impact_function_dataset definition

-- Drop table

-- DROP TABLE "data".health_impact_function_dataset;

CREATE TABLE "data".health_impact_function_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT health_impact_function_dataset_pkey PRIMARY KEY (id)
);


-- "data".health_impact_function_group definition

-- Drop table

-- DROP TABLE "data".health_impact_function_group;

CREATE TABLE "data".health_impact_function_group (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	help_text text NULL,
	CONSTRAINT health_impact_function_group_pkey PRIMARY KEY (id)
);


-- "data".health_impact_function_group_member definition

-- Drop table

-- DROP TABLE "data".health_impact_function_group_member;

CREATE TABLE "data".health_impact_function_group_member (
	health_impact_function_group_id int4 NULL,
	health_impact_function_id int4 NULL
);


-- "data".hif_result definition

-- Drop table

-- DROP TABLE "data".hif_result;

CREATE TABLE "data".hif_result (
	hif_result_dataset_id int4 NULL,
	hif_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population float8 NULL,
	delta_aq float8 NULL,
	baseline_aq float8 NULL,
	scenario_aq float8 NULL,
	incidence float8 NULL,
	"result" float8 NULL,
	baseline float8 NULL,
	result_mean float8 NULL,
	standard_dev float8 NULL,
	result_variance float8 NULL,
	pct_2_5 float8 NULL,
	pct_97_5 float8 NULL,
	percentiles _float8 NULL
);
CREATE INDEX hif_result_hif_result_dataset_id_idx ON data.hif_result USING btree (hif_result_dataset_id, hif_id, grid_col, grid_row, grid_cell_id, result, population, baseline_aq, scenario_aq, delta_aq, incidence, result_mean, baseline, standard_dev, result_variance, pct_2_5, pct_97_5);


-- "data".hif_result_dataset definition

-- Drop table

-- DROP TABLE "data".hif_result_dataset;

CREATE TABLE "data".hif_result_dataset (
	id serial4 NOT NULL,
	task_uuid text NULL,
	"name" text NULL,
	population_dataset_id int4 NULL,
	population_year int4 NULL,
	baseline_aq_layer_id int4 NULL,
	scenario_aq_layer_id int4 NULL,
	task_log json NULL,
	CONSTRAINT hif_result_dataset_pkey PRIMARY KEY (id)
);


-- "data".hif_result_function_config definition

-- Drop table

-- DROP TABLE "data".hif_result_function_config;

CREATE TABLE "data".hif_result_function_config (
	hif_result_dataset_id int4 NULL,
	hif_id int4 NULL,
	start_age int4 NULL,
	end_age int4 NULL,
	incidence_dataset_id int4 NULL,
	prevalence_dataset_id int4 NULL,
	variable_dataset_id int4 NULL,
	race_id int4 NULL,
	gender_id int4 NULL,
	ethnicity_id int4 NULL,
	metric_id int4 NULL,
	seasonal_metric_id int4 NULL,
	metric_statistic int4 NULL
);


-- "data".income_growth_adj_dataset definition

-- Drop table

-- DROP TABLE "data".income_growth_adj_dataset;

CREATE TABLE "data".income_growth_adj_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT income_growth_adj_dataset_pkey PRIMARY KEY (id)
);


-- "data".inflation_dataset definition

-- Drop table

-- DROP TABLE "data".inflation_dataset;

CREATE TABLE "data".inflation_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT inflation_dataset_pkey PRIMARY KEY (id)
);


-- "data".inflation_entry definition

-- Drop table

-- DROP TABLE "data".inflation_entry;

CREATE TABLE "data".inflation_entry (
	inflation_dataset_id int4 NULL,
	entry_year int4 NULL,
	all_goods_index float8 NULL,
	medical_cost_index float8 NULL,
	wage_index float8 NULL
);


-- "data".pollutant definition

-- Drop table

-- DROP TABLE "data".pollutant;

CREATE TABLE "data".pollutant (
	id serial4 NOT NULL,
	"name" text NULL,
	observation_type int2 NULL,
	friendly_name text NULL,
	help_text text NULL,
	CONSTRAINT pollutant_pkey PRIMARY KEY (id)
);


-- "data".pop_config definition

-- Drop table

-- DROP TABLE "data".pop_config;

CREATE TABLE "data".pop_config (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT pop_config_pkey PRIMARY KEY (id)
);


-- "data".population_value definition

-- Drop table

-- DROP TABLE "data".population_value;

CREATE TABLE "data".population_value (
	pop_entry_id int4 NULL,
	grid_cell_id int8 NULL,
	pop_value float8 NULL
);


-- "data".race definition

-- Drop table

-- DROP TABLE "data".race;

CREATE TABLE "data".race (
	id serial4 NOT NULL,
	"name" text NULL,
	CONSTRAINT race_pkey PRIMARY KEY (id)
);


-- "data".settings definition

-- Drop table

-- DROP TABLE "data".settings;

CREATE TABLE "data".settings (
	"key" text NULL,
	value_text text NULL,
	value_int int4 NULL
);


-- "data".statistic_type definition

-- Drop table

-- DROP TABLE "data".statistic_type;

CREATE TABLE "data".statistic_type (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT statistic_type_pkey PRIMARY KEY (id)
);


-- "data".task_complete definition

-- Drop table

-- DROP TABLE "data".task_complete;

CREATE TABLE "data".task_complete (
	task_id serial4 NOT NULL,
	task_uuid text NULL,
	task_parent_uuid text NULL,
	task_user_identifier text NULL,
	task_priority int4 NULL,
	task_name text NULL,
	task_description text NULL,
	task_type text NULL,
	task_parameters text NULL,
	task_results text NULL,
	task_successful bool NULL,
	task_complete_message text NULL,
	task_submitted_date timestamp NULL,
	task_started_date timestamp NULL,
	task_completed_date timestamp NULL,
	CONSTRAINT task_complete_pkey PRIMARY KEY (task_id)
);
CREATE INDEX task_complete_on_date ON data.task_complete USING btree (task_completed_date);
CREATE INDEX task_complete_on_uuid ON data.task_complete USING btree (task_uuid);


-- "data".task_config definition

-- Drop table

-- DROP TABLE "data".task_config;

CREATE TABLE "data".task_config (
	id serial4 NOT NULL,
	"name" text NULL,
	"type" text NULL,
	parameters json NULL,
	CONSTRAINT task_config_dataset_pkey PRIMARY KEY (id)
);


-- "data".task_queue definition

-- Drop table

-- DROP TABLE "data".task_queue;

CREATE TABLE "data".task_queue (
	task_id serial4 NOT NULL,
	task_user_identifier text NULL,
	task_priority int4 NULL,
	task_uuid text NULL,
	task_parent_uuid text NULL,
	task_name text NULL,
	task_description text NULL,
	task_type text NULL,
	task_parameters text NULL,
	task_percentage int4 NULL,
	task_message text NULL,
	task_in_process bool NULL DEFAULT false,
	task_submitted_date timestamp NULL,
	task_started_date timestamp NULL,
	CONSTRAINT task_queue_pkey PRIMARY KEY (task_id)
);
CREATE INDEX task_queue_on_date ON data.task_queue USING btree (task_submitted_date);
CREATE INDEX task_queue_on_priority_submitted_date ON data.task_queue USING btree (task_priority, task_submitted_date);
CREATE INDEX task_queue_on_uuid ON data.task_queue USING btree (task_uuid);


-- "data".task_worker definition

-- Drop table

-- DROP TABLE "data".task_worker;

CREATE TABLE "data".task_worker (
	task_id serial4 NOT NULL,
	task_worker_uuid text NULL,
	task_uuid text NULL,
	last_heartbeat_date timestamp NULL,
	CONSTRAINT task_worker_pkey PRIMARY KEY (task_id)
);
CREATE INDEX task_worker_on_task_uuid ON data.task_worker USING btree (task_uuid);
CREATE INDEX task_worker_on_uuid ON data.task_worker USING btree (task_worker_uuid);


-- "data".valuation_function definition

-- Drop table

-- DROP TABLE "data".valuation_function;

CREATE TABLE "data".valuation_function (
	id serial4 NOT NULL,
	valuation_dataset_id int4 NULL,
	endpoint_group_id int4 NULL,
	endpoint_id int4 NULL,
	qualifier text NULL,
	reference text NULL,
	start_age int4 NULL,
	end_age int4 NULL,
	function_text text NULL,
	val_a float8 NULL,
	name_a text NULL,
	dist_a text NULL,
	p1a float8 NULL,
	p2a float8 NULL,
	val_b float8 NULL,
	name_b text NULL,
	val_c float8 NULL,
	name_c text NULL,
	val_d float8 NULL,
	name_d text NULL,
	CONSTRAINT valuation_function_pkey PRIMARY KEY (id)
);


-- "data".valuation_function_dataset definition

-- Drop table

-- DROP TABLE "data".valuation_function_dataset;

CREATE TABLE "data".valuation_function_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT valuation_function_dataset_pkey PRIMARY KEY (id)
);


-- "data".valuation_result definition

-- Drop table

-- DROP TABLE "data".valuation_result;

CREATE TABLE "data".valuation_result (
	valuation_result_dataset_id int4 NULL,
	vf_id int4 NULL,
	hif_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population float8 NULL,
	"result" float8 NULL,
	result_mean float8 NULL,
	standard_dev float8 NULL,
	result_variance float8 NULL,
	pct_2_5 float8 NULL,
	pct_97_5 float8 NULL,
	percentiles _float8 NULL
);
CREATE INDEX valuation_result_valuation_result_dataset_id_idx ON data.valuation_result USING btree (valuation_result_dataset_id, vf_id, hif_id, grid_col, grid_row, grid_cell_id, population, result, result_mean, standard_dev, result_variance, pct_2_5, pct_97_5);


-- "data".valuation_result_dataset definition

-- Drop table

-- DROP TABLE "data".valuation_result_dataset;

CREATE TABLE "data".valuation_result_dataset (
	id serial4 NOT NULL,
	task_uuid text NULL,
	hif_result_dataset_id int4 NULL,
	"name" text NULL,
	variable_dataset_id int4 NULL,
	task_log json NULL,
	CONSTRAINT valuation_result_dataset_pkey PRIMARY KEY (id)
);


-- "data".valuation_result_function_config definition

-- Drop table

-- DROP TABLE "data".valuation_result_function_config;

CREATE TABLE "data".valuation_result_function_config (
	valuation_result_dataset_id int4 NULL,
	vf_id int4 NULL,
	hif_id int4 NULL
);


-- "data".variable_dataset definition

-- Drop table

-- DROP TABLE "data".variable_dataset;

CREATE TABLE "data".variable_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	grid_definition_id int4 NULL,
	CONSTRAINT variable_dataset_pkey PRIMARY KEY (id)
);


-- "data".variable_entry definition

-- Drop table

-- DROP TABLE "data".variable_entry;

CREATE TABLE "data".variable_entry (
	id serial4 NOT NULL,
	variable_dataset_id int4 NULL,
	"name" text NOT NULL,
	CONSTRAINT variable_entry_pkey PRIMARY KEY (id)
);


-- "data".variable_value definition

-- Drop table

-- DROP TABLE "data".variable_value;

CREATE TABLE "data".variable_value (
	variable_entry_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	value float8 NULL,
	grid_cell_id int8 NULL
);


-- "data".age_range definition

-- Drop table

-- DROP TABLE "data".age_range;

CREATE TABLE "data".age_range (
	id serial4 NOT NULL,
	pop_config_id int4 NULL,
	"name" text NOT NULL,
	start_age int2 NULL,
	end_age int2 NULL,
	CONSTRAINT age_range_pkey PRIMARY KEY (id),
	CONSTRAINT age_range_pop_config_id_fkey FOREIGN KEY (pop_config_id) REFERENCES "data".pop_config(id)
);


-- "data".air_quality_layer definition

-- Drop table

-- DROP TABLE "data".air_quality_layer;

CREATE TABLE "data".air_quality_layer (
	id serial4 NOT NULL,
	"name" text NULL,
	pollutant_id int4 NULL,
	grid_definition_id int4 NULL,
	"locked" bool NULL DEFAULT false,
	CONSTRAINT air_quality_layer_pkey PRIMARY KEY (id),
	CONSTRAINT air_quality_layer_grid_definition_id_fkey FOREIGN KEY (grid_definition_id) REFERENCES "data".grid_definition(id),
	CONSTRAINT air_quality_layer_pollutant_id_fkey FOREIGN KEY (pollutant_id) REFERENCES "data".pollutant(id)
);


-- "data".endpoint definition

-- Drop table

-- DROP TABLE "data".endpoint;

CREATE TABLE "data".endpoint (
	id serial4 NOT NULL,
	endpoint_group_id int2 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT endpoint_pkey PRIMARY KEY (id),
	CONSTRAINT endpoint_endpoint_group_id_fkey FOREIGN KEY (endpoint_group_id) REFERENCES "data".endpoint_group(id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- "data".incidence_dataset definition

-- Drop table

-- DROP TABLE "data".incidence_dataset;

CREATE TABLE "data".incidence_dataset (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	grid_definition_id int4 NULL,
	CONSTRAINT incidence_dataset_pkey PRIMARY KEY (id),
	CONSTRAINT incidence_dataset_grid_definition_id_fkey FOREIGN KEY (grid_definition_id) REFERENCES "data".grid_definition(id)
);


-- "data".incidence_entry definition

-- Drop table

-- DROP TABLE "data".incidence_entry;

CREATE TABLE "data".incidence_entry (
	id serial4 NOT NULL,
	incidence_dataset_id int4 NULL,
	"year" int4 NULL,
	endpoint_group_id int4 NULL,
	endpoint_id int4 NULL,
	race_id int4 NULL,
	gender_id int4 NULL,
	start_age int2 NULL,
	end_age int2 NULL,
	prevalence bool NULL,
	ethnicity_id int4 NULL,
	CONSTRAINT incidence_entry_pkey PRIMARY KEY (id),
	CONSTRAINT incidence_entry_incidence_dataset_id_fkey FOREIGN KEY (incidence_dataset_id) REFERENCES "data".incidence_dataset(id)
);


-- "data".incidence_value definition

-- Drop table

-- DROP TABLE "data".incidence_value;

CREATE TABLE "data".incidence_value (
	incidence_entry_id int4 NULL,
	grid_cell_id int8 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	value float8 NULL,
	CONSTRAINT incidence_value_incidence_entry_id_fkey FOREIGN KEY (incidence_entry_id) REFERENCES "data".incidence_entry(id)
);
CREATE INDEX incidence_value_incidence_entry_id_idx ON data.incidence_value USING btree (incidence_entry_id, grid_cell_id, value);


-- "data".income_growth_adj_factor definition

-- Drop table

-- DROP TABLE "data".income_growth_adj_factor;

CREATE TABLE "data".income_growth_adj_factor (
	id serial4 NOT NULL,
	income_growth_adj_dataset_id int2 NOT NULL,
	growth_year int2 NOT NULL,
	mean_value float8 NOT NULL,
	endpoint_group_id int2 NOT NULL,
	CONSTRAINT income_growth_adj_factor_pkey PRIMARY KEY (id),
	CONSTRAINT income_growth_adj_factor_endpoint_group_id_fkey FOREIGN KEY (endpoint_group_id) REFERENCES "data".endpoint_group(id),
	CONSTRAINT income_growth_adj_factor_income_growth_adj_dataset_id_fkey FOREIGN KEY (income_growth_adj_dataset_id) REFERENCES "data".income_growth_adj_dataset(id)
);


-- "data".pollutant_metric definition

-- Drop table

-- DROP TABLE "data".pollutant_metric;

CREATE TABLE "data".pollutant_metric (
	id serial4 NOT NULL,
	pollutant_id int4 NULL,
	"name" text NULL,
	hourly_metric_generation int2 NULL,
	window_size int2 NULL,
	window_statistic int2 NULL,
	start_hour int2 NULL,
	end_hour int2 NULL,
	daily_statistic int2 NULL,
	CONSTRAINT pollutant_metric_pkey PRIMARY KEY (id),
	CONSTRAINT pollutant_metric_pollutant_id_fkey FOREIGN KEY (pollutant_id) REFERENCES "data".pollutant(id)
);


-- "data".pop_config_ethnicity definition

-- Drop table

-- DROP TABLE "data".pop_config_ethnicity;

CREATE TABLE "data".pop_config_ethnicity (
	pop_config_id int4 NULL,
	ethnicity_id int4 NULL,
	CONSTRAINT pop_config_ethnicity_ethnicity_id_fkey FOREIGN KEY (ethnicity_id) REFERENCES "data".ethnicity(id),
	CONSTRAINT pop_config_ethnicity_pop_config_id_fkey FOREIGN KEY (pop_config_id) REFERENCES "data".pop_config(id)
);


-- "data".pop_config_gender definition

-- Drop table

-- DROP TABLE "data".pop_config_gender;

CREATE TABLE "data".pop_config_gender (
	pop_config_id int4 NULL,
	gender_id int4 NULL,
	CONSTRAINT pop_config_gender_gender_id_fkey FOREIGN KEY (gender_id) REFERENCES "data".gender(id),
	CONSTRAINT pop_config_gender_pop_config_id_fkey FOREIGN KEY (pop_config_id) REFERENCES "data".pop_config(id)
);


-- "data".pop_config_race definition

-- Drop table

-- DROP TABLE "data".pop_config_race;

CREATE TABLE "data".pop_config_race (
	pop_config_id int4 NULL,
	race_id int4 NULL,
	CONSTRAINT pop_config_race_pop_config_id_fkey FOREIGN KEY (pop_config_id) REFERENCES "data".pop_config(id),
	CONSTRAINT pop_config_race_race_id_fkey FOREIGN KEY (race_id) REFERENCES "data".race(id)
);


-- "data".population_dataset definition

-- Drop table

-- DROP TABLE "data".population_dataset;

CREATE TABLE "data".population_dataset (
	id serial4 NOT NULL,
	"name" text NULL,
	pop_config_id int4 NULL,
	grid_definition_id int4 NULL,
	apply_growth int4 NULL,
	CONSTRAINT population_dataset_pkey PRIMARY KEY (id),
	CONSTRAINT population_dataset_pop_config_id_fkey FOREIGN KEY (pop_config_id) REFERENCES "data".pop_config(id)
);


-- "data".population_entry definition

-- Drop table

-- DROP TABLE "data".population_entry;

CREATE TABLE "data".population_entry (
	id serial4 NOT NULL,
	pop_dataset_id int4 NULL,
	race_id int4 NULL,
	ethnicity_id int4 NULL,
	gender_id int4 NULL,
	age_range_id int4 NULL,
	pop_year int2 NULL,
	CONSTRAINT population_entry_pkey PRIMARY KEY (id),
	CONSTRAINT population_entry_pop_dataset_id_fkey FOREIGN KEY (pop_dataset_id) REFERENCES "data".population_dataset(id)
);
CREATE INDEX population_entry_id_idx ON data.population_entry USING btree (id, pop_dataset_id, race_id, ethnicity_id, gender_id, age_range_id, pop_year);


-- "data".seasonal_metric definition

-- Drop table

-- DROP TABLE "data".seasonal_metric;

CREATE TABLE "data".seasonal_metric (
	id serial4 NOT NULL,
	metric_id int4 NULL,
	"name" text NULL,
	CONSTRAINT seasonal_metric_pkey PRIMARY KEY (id),
	CONSTRAINT seasonal_metric_metric_id_fkey FOREIGN KEY (metric_id) REFERENCES "data".pollutant_metric(id)
);


-- "data".seasonal_metric_season definition

-- Drop table

-- DROP TABLE "data".seasonal_metric_season;

CREATE TABLE "data".seasonal_metric_season (
	id serial4 NOT NULL,
	seasonal_metric_id int4 NULL,
	start_day int2 NULL,
	end_day int2 NULL,
	seasonal_metric_type int2 NULL,
	metric_function text NULL,
	CONSTRAINT seasonal_metric_season_pkey PRIMARY KEY (id),
	CONSTRAINT seasonal_metric_season_seasonal_metric_id_fkey FOREIGN KEY (seasonal_metric_id) REFERENCES "data".seasonal_metric(id)
);


-- "data".air_quality_cell definition

-- Drop table

-- DROP TABLE "data".air_quality_cell;

CREATE TABLE "data".air_quality_cell (
	air_quality_layer_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	metric_id int4 NULL,
	seasonal_metric_id int4 NULL,
	annual_statistic_id int4 NULL,
	value float8 NULL,
	CONSTRAINT air_quality_cell_air_quality_layer_id_fkey FOREIGN KEY (air_quality_layer_id) REFERENCES "data".air_quality_layer(id),
	CONSTRAINT air_quality_cell_metric_id_fkey FOREIGN KEY (metric_id) REFERENCES "data".pollutant_metric(id),
	CONSTRAINT air_quality_cell_seasonal_metric_id_fkey FOREIGN KEY (seasonal_metric_id) REFERENCES "data".seasonal_metric(id)
);



CREATE OR REPLACE FUNCTION data.get_hif_results(_dataset_id integer, _hif_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, hif_id integer, point_estimate double precision, population double precision, baseline_aq double precision, scenario_aq double precision, delta_aq double precision, incidence double precision, mean double precision, baseline double precision, standard_dev double precision, variance double precision, pct_2_5 double precision, pct_97_5 double precision, percentiles double precision[])
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
begin
-- Throw exception if data_set_id is NULL
	if _dataset_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _dataset_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _output_grid_definition_id is NULL
	if _output_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _output_grid_definition_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if we can't determine the _source_grid_definition_id
	select aql.grid_definition_id from data.hif_result_dataset hrd join data.air_quality_layer aql on aql.id = hrd.baseline_aq_layer_id where hrd.id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Could not determine grid definition for result dataset';
	end if;
-- Throw exception if source data set grid ID is != output grid definition ID AND
-- there is no mapping in crosswalk_dataset to do a conversion
	if (_source_grid_definition_id != _output_grid_definition_id) then
		SELECT
			crosswalk_dataset.id 
		FROM
			data.crosswalk_dataset			 
		WHERE 
			crosswalk_dataset.source_grid_id = _source_grid_definition_id AND
			crosswalk_dataset.target_grid_id = _output_grid_definition_id
		LIMIT 1
		into
			_crosswalk_dataset_id;
		if _crosswalk_dataset_id IS NULL then
			RAISE EXCEPTION 'Crosswalk does not exist. Cannot walk from grid ID % to grid ID %', _source_grid_definition_id, _output_grid_definition_id;
		end if;
		-- CROSSWALK CODE GOES HERE	
		return query	
			SELECT 
       			ce.target_col, 
       			ce.target_row,
       			hr.hif_id,
       			sum(hr."result" * ce.percentage) as point_estimate,
       			sum(hr.population * ce.percentage) as population,
       			sum(hr.baseline_aq * ce.percentage * hr.population) / sum(hr.population) as baseline_aq,
       			sum(hr.scenario_aq * ce.percentage * hr.population) / sum(hr.population) as scenario_aq,
       			sum(hr.delta_aq * ce.percentage * hr.population) / sum(hr.population) as delta_aq,
       			sum(hr.incidence * ce.percentage) as incidence,
       			sum(hr.result_mean * ce.percentage) as mean,
				sum(hr.baseline * ce.percentage) as baseline,
				sum(hr.standard_dev * ce.percentage) as standard_dev,
       			sum(hr.result_variance * ce.percentage) as "variance",
       			sum(hr.pct_2_5 * ce.percentage) as pct_2_5,
       			sum(hr.pct_97_5 * ce.percentage) as pct_97_5,
       			ARRAY[
       				sum(hr.percentiles[1] * ce.percentage),
       				sum(hr.percentiles[2] * ce.percentage),
       				sum(hr.percentiles[3] * ce.percentage),
       				sum(hr.percentiles[4] * ce.percentage),
       				sum(hr.percentiles[5] * ce.percentage),
       				sum(hr.percentiles[6] * ce.percentage),
       				sum(hr.percentiles[7] * ce.percentage),
       				sum(hr.percentiles[8] * ce.percentage),
       				sum(hr.percentiles[9] * ce.percentage),
       				sum(hr.percentiles[10] * ce.percentage),
       				sum(hr.percentiles[11] * ce.percentage),
       				sum(hr.percentiles[12] * ce.percentage),
       				sum(hr.percentiles[13] * ce.percentage),
       				sum(hr.percentiles[14] * ce.percentage),
       				sum(hr.percentiles[15] * ce.percentage),
       				sum(hr.percentiles[16] * ce.percentage),
       				sum(hr.percentiles[17] * ce.percentage),
       				sum(hr.percentiles[18] * ce.percentage),
       				sum(hr.percentiles[19] * ce.percentage),
       				sum(hr.percentiles[20] * ce.percentage)
       			] as percentiles
  			FROM 
       			data.hif_result hr 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = hr.grid_cell_id					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
			GROUP BY
				ce.target_col, 
       			ce.target_row,
       			hr.hif_id
			ORDER BY
				ce.target_col, 
       			ce.target_row,
       			hr.hif_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			SELECT 
       			hr.grid_col, 
       			hr.grid_row,
       			hr.hif_id,
       			hr."result" as point_estimate,
       			hr.population as population,
       			hr.baseline_aq,
       			hr.scenario_aq,
       			hr.delta_aq,
       			hr.incidence,
       			hr.result_mean as mean,
				hr.baseline as baseline,
				hr.standard_dev as standard_dev,
       			hr.result_variance as "variance",
       			hr.pct_2_5 as pct_2_5,
       			hr.pct_97_5 as pct_97_5, 
       			hr.percentiles 
  			FROM 
       			data.hif_result hr 					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
			ORDER BY
       			hr.grid_col, 
       			hr.grid_row,
       			hr.hif_id;
	end if;	
end
$function$
;

CREATE OR REPLACE FUNCTION data.get_incidence(_dataset_id integer, _year integer, _endpoint_id integer, _race_id integer[], _ethnicity_id integer[], _gender_id integer[], _start_age smallint, _end_age smallint, _group_by_race boolean, _group_by_ethnicity boolean, _group_by_gender boolean, _group_by_age_range boolean, _output_grid_definition_id integer)
 RETURNS TABLE(grid_cell_id bigint, race_id integer, ethnicity_id integer, gender_id integer, start_age smallint, end_age smallint, value double precision)
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
begin
-- Throw exception if data_set_id is NULL
	if _dataset_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _dataset_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _output_grid_definition_id is NULL
	if _output_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _output_grid_definition_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _year is NULL
	if _year IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _year'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _endpoint_id is NULL
	if _endpoint_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _endpoint_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if data_set_id is not found in data.incidence_dataset.id
	SELECT grid_definition_id FROM data.incidence_dataset WHERE id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Value not found in incidence_dataset';
	end if;
-- Throw exception if source data set grid ID is != output grid definition ID AND
-- there is no mapping in crosswalk_dataset to do a conversion
	if (_source_grid_definition_id != _output_grid_definition_id) then
		SELECT
			crosswalk_dataset.id 
		FROM
			data.crosswalk_dataset			 
		WHERE 
			crosswalk_dataset.target_grid_id = _source_grid_definition_id AND
			crosswalk_dataset.source_grid_id = _output_grid_definition_id
		LIMIT 1
		into
			_crosswalk_dataset_id;
		if _crosswalk_dataset_id IS NULL then
			RAISE EXCEPTION 'Crosswalk does not exist. Cannot walk from grid ID % to grid ID %', _source_grid_definition_id, _output_grid_definition_id;
		end if;
		-- CROSSWALK CODE GOES HERE	
		return query	
		select
		  ce.source_grid_cell_id as grid_cell_id,
		  case when _group_by_race then ie.race_id else null end, 
		  case when _group_by_ethnicity then ie.ethnicity_id else null end,
		  case when _group_by_gender then ie.gender_id else null end,
		  case when _group_by_age_range then ie.start_age else null end,
		  case when _group_by_age_range then ie.end_age else null end,
		  case when _dataset_id = 3 then avg(iv.value * ce.percentage) else sum(iv.value * ce.percentage) end as value
		from data.incidence_entry ie
		  join data.incidence_value iv on iv.incidence_entry_id = ie.id
		  join data.crosswalk_entry ce on iv.grid_cell_id = ce.target_grid_cell_id and ce.crosswalk_id = _crosswalk_dataset_id
		where (
		  ie.incidence_dataset_id = _dataset_id
		  and ie.endpoint_id = _endpoint_id
		  and ie.year = _year
		  and (_race_id is null or ie.race_id = any(_race_id)) 
		  and (_ethnicity_id is null or ie.ethnicity_id = any(_ethnicity_id))
		  and (_gender_id is null or ie.gender_id = any(_gender_id))
		  and (_start_age is null or ie.end_age >= _start_age)
		  and (_end_age is null or ie.start_age <= _end_age)
		)
		group by
		  ce.source_grid_cell_id,
		  case when _group_by_race then ie.race_id else null end,
		  case when _group_by_ethnicity then ie.ethnicity_id else null end,
		  case when _group_by_gender then ie.gender_id else null end,
		  case when _group_by_age_range then ie.start_age else null end,
		  case when _group_by_age_range then ie.end_age else null end
		order by
			ce.source_grid_cell_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
		select
		  iv.grid_cell_id,
		  case when _group_by_race then ie.race_id else null end, 
		  case when _group_by_ethnicity then ie.ethnicity_id else null end,
		  case when _group_by_gender then ie.gender_id else null end,
		  case when _group_by_age_range then ie.start_age else null end,
		  case when _group_by_age_range then ie.end_age else null end,
		  case when _dataset_id = 3 then avg(iv.value * ce.percentage) else sum(iv.value * ce.percentage) end as value
		from data.incidence_entry ie
		  join data.incidence_value iv on iv.incidence_entry_id = ie.id
		where (
		  ie.incidence_dataset_id = _dataset_id
		  and ie.endpoint_id = _endpoint_id
		  and ie.year = _year
		  and (_race_id is null or ie.race_id = any(_race_id)) 
		  and (_ethnicity_id is null or ie.ethnicity_id = any(_ethnicity_id))
		  and (_gender_id is null or ie.gender_id = any(_gender_id))
		  and (_start_age is null or ie.end_age >= _start_age)
		  and (_end_age is null or ie.start_age <= _end_age)
		)
		group by
		  iv.grid_cell_id,
		  case when _group_by_race then ie.race_id else null end,
		  case when _group_by_ethnicity then ie.ethnicity_id else null end,
		  case when _group_by_gender then ie.gender_id else null end,
		  case when _group_by_age_range then ie.start_age else null end,
		  case when _group_by_age_range then ie.end_age else null end
		order by
			iv.grid_cell_id;
	end if;	
end
$function$
;

CREATE OR REPLACE FUNCTION data.get_population(_dataset_id integer, _year integer, _race_id integer[], _ethnicity_id integer[], _gender_id integer[], _age_range_id integer[], _group_by_race boolean, _group_by_ethnicity boolean, _group_by_gender boolean, _group_by_age_range boolean, _output_grid_definition_id integer)
 RETURNS TABLE(grid_cell_id bigint, race_id integer, ethnicity_id integer, gender_id integer, age_range_id integer, pop_value double precision)
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
begin
-- Throw exception if data_set_id is NULL
	if _dataset_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _dataset_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _output_grid_definition_id is NULL
	if _output_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _output_grid_definition_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _year is NULL
	if _year IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _year'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if data_set_id is not found in data.population_dataset.id
	SELECT grid_definition_id FROM data.population_dataset WHERE id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Value not found in population_dataset';
	end if;
-- Throw exception if source data set grid ID is != output grid definition ID AND
-- there is no mapping in crosswalk_dataset to do a conversion
	if (_source_grid_definition_id != _output_grid_definition_id) then
		SELECT
			crosswalk_dataset.id 
		FROM
			data.crosswalk_dataset			 
		WHERE 
			crosswalk_dataset.source_grid_id = _source_grid_definition_id AND
			crosswalk_dataset.target_grid_id = _output_grid_definition_id
		LIMIT 1
		into
			_crosswalk_dataset_id;
		if _crosswalk_dataset_id IS NULL then
			RAISE EXCEPTION 'Crosswalk does not exist. Cannot walk from grid ID % to grid ID %', _source_grid_definition_id, _output_grid_definition_id;
		end if;
		-- CROSSWALK CODE GOES HERE	
		return query	
			SELECT 
       			c.target_grid_cell_id, 
				case when _group_by_race then e.race_id else null end, 
				case when _group_by_ethnicity then e.ethnicity_id else null end,
				case when _group_by_gender then e.gender_id else null end,
				case when _group_by_age_range then e.age_range_id else null end,
       			SUM(v.pop_value * c.percentage)
  			FROM data.population_entry e
			JOIN data.population_value v on e.id = v.pop_entry_id
			JOIN data.crosswalk_entry c On
	   				c.crosswalk_id = _crosswalk_dataset_id AND
					c.source_grid_cell_id = v.grid_cell_id					
 			WHERE
				e.pop_dataset_id = _dataset_id and
				e.pop_year = _year AND
				(_race_id IS NULL OR e.race_id = ANY(_race_id)) AND
				(_ethnicity_id IS NULL OR e.ethnicity_id =  ANY(_ethnicity_id)) AND
				(_gender_id IS NULL OR e.gender_id = ANY(_gender_id)) AND
				(_age_range_id IS NULL OR e.age_range_id = ANY(_age_range_id))
			GROUP BY
				c.target_grid_cell_id,
				case when _group_by_race then e.race_id else null end, 
				case when _group_by_ethnicity then e.ethnicity_id else null end,
				case when _group_by_gender then e.gender_id else null end,
				case when _group_by_age_range then e.age_range_id else null end
			ORDER BY
				c.target_grid_cell_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			select
				v.grid_cell_id,
				case when _group_by_race then e.race_id else null end, 
				case when _group_by_ethnicity then e.ethnicity_id else null end,
				case when _group_by_gender then e.gender_id else null end,
				case when _group_by_age_range then e.age_range_id else null end,
				sum(v.pop_value)
			from data.population_entry e
			join data.population_value v on e.id = v.pop_entry_id
			where
				e.pop_dataset_id = _dataset_id and
				e.pop_year = _year AND
				(_race_id IS NULL OR e.race_id = ANY (_race_id)) AND
				(_ethnicity_id IS NULL OR e.ethnicity_id = ANY (_ethnicity_id)) AND
				(_gender_id IS NULL OR e.gender_id = ANY (_gender_id)) AND
				(_age_range_id IS NULL OR e.age_range_id = ANY (_age_range_id))
			GROUP BY
				v.grid_cell_id,
				case when _group_by_race then e.race_id else null end, 
				case when _group_by_ethnicity then e.ethnicity_id else null end,
				case when _group_by_gender then e.gender_id else null end,
				case when _group_by_age_range then e.age_range_id else null end
			ORDER BY
				v.grid_cell_id;
	end if;	
end
$function$
;

CREATE OR REPLACE FUNCTION data.get_valuation_results(_dataset_id integer, _hif_id integer[], _vf_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, hif_id integer, vf_id integer, point_estimate double precision, mean double precision, standard_dev double precision, variance double precision, pct_2_5 double precision, pct_97_5 double precision)
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
begin
-- Throw exception if data_set_id is NULL
	if _dataset_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _dataset_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _output_grid_definition_id is NULL
	if _output_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _output_grid_definition_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if we can't determine the _source_grid_definition_id
	select aql.grid_definition_id from data.valuation_result_dataset vrd join data.hif_result_dataset hrd on vrd.hif_result_dataset_id = hrd.id join data.air_quality_layer aql on aql.id = hrd.baseline_aq_layer_id where vrd.id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Could not determine grid definition for result dataset';
	end if;
-- Throw exception if source data set grid ID is != output grid definition ID AND
-- there is no mapping in crosswalk_dataset to do a conversion
	if (_source_grid_definition_id != _output_grid_definition_id) then
		SELECT
			crosswalk_dataset.id 
		FROM
			data.crosswalk_dataset			 
		WHERE 
			crosswalk_dataset.source_grid_id = _source_grid_definition_id AND
			crosswalk_dataset.target_grid_id = _output_grid_definition_id
		LIMIT 1
		into
			_crosswalk_dataset_id;
		if _crosswalk_dataset_id IS NULL then
			RAISE EXCEPTION 'Crosswalk does not exist. Cannot walk from grid ID % to grid ID %', _source_grid_definition_id, _output_grid_definition_id;
		end if;
		-- CROSSWALK CODE GOES HERE	
		return query	
			SELECT 
       			ce.target_col, 
       			ce.target_row,
       			vr.hif_id,
       			vr.vf_id,
       			sum(vr."result" * ce.percentage) as point_estimate,
       			sum(vr.result_mean * ce.percentage) as mean,
				sum(vr.standard_dev * ce.percentage) as standard_dev,
       			sum(vr.result_variance * ce.percentage) as "variance",
       			sum(vr.pct_2_5 * ce.percentage) as pct_2_5,
       			sum(vr.pct_97_5 * ce.percentage) as pct_97_5 
  			FROM 
       			data.valuation_result vr 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = vr.grid_cell_id					
 			WHERE
				vr.valuation_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR vr.hif_id = ANY(_hif_id)) and
				(_vf_id IS NULL OR vr.hif_id = ANY(_vf_id))
			GROUP BY
				ce.target_col, 
       			ce.target_row,
       			vr.hif_id,
       			vr.vf_id 
			ORDER BY
				ce.target_col, 
       			ce.target_row,
       			vr.hif_id,
       			vr.vf_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			SELECT 
       			vr.grid_col, 
       			vr.grid_row,
       			vr.hif_id,
       			vr.vf_id,
       			vr."result" as point_estimate,
       			vr.result_mean as mean,
				vr.standard_dev as standard_dev,
       			vr.result_variance as "variance",
       			vr.pct_2_5 as pct_2_5,
       			vr.pct_97_5 as pct_97_5 
  			FROM 
       			data.valuation_result vr 					
 			WHERE
				vr.valuation_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR vr.hif_id = ANY(_hif_id)) and
				(_vf_id IS NULL OR vr.hif_id = ANY(_vf_id))
			ORDER BY
       			vr.grid_col, 
       			vr.grid_row,
       			vr.hif_id,
       			vr.vf_id;
	end if;	
end
$function$
;

CREATE OR REPLACE FUNCTION data.get_variable(_dataset_id integer, _variable_name text[], _output_grid_definition_id integer)
 RETURNS TABLE(variable_name text, grid_cell_id bigint, value double precision)
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
begin
-- Throw exception if data_set_id is NULL
	if _dataset_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _dataset_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if _output_grid_definition_id is NULL
	if _output_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - _output_grid_definition_id'
			USING HINT = 'Parameter cannot be NULL';
	end if;
-- Throw exception if data_set_id is not found in data.variable_dataset.id
	SELECT grid_definition_id FROM data.incidence_dataset WHERE id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Value not found in variable_dataset';
	end if;
-- Throw exception if source data set grid ID is != output grid definition ID AND
-- there is no mapping in crosswalk_dataset to do a conversion
	if (_source_grid_definition_id != _output_grid_definition_id) then
		SELECT
			crosswalk_dataset.id 
		FROM
			data.crosswalk_dataset			 
		WHERE 
			crosswalk_dataset.target_grid_id = _source_grid_definition_id AND
			crosswalk_dataset.source_grid_id = _output_grid_definition_id
		LIMIT 1
		into
			_crosswalk_dataset_id;
		if _crosswalk_dataset_id IS NULL then
			RAISE EXCEPTION 'Crosswalk does not exist. Cannot walk from grid ID % to grid ID %', _source_grid_definition_id, _output_grid_definition_id;
		end if;
		-- CROSSWALK CODE GOES HERE	
		return query	
		select
		  ve."name",
		  ce.source_grid_cell_id as grid_cell_id,
		  avg(vv.value) as value
		from data.variable_entry ve
		  join data.variable_value vv on vv.variable_entry_id = ve.id 
		  join data.crosswalk_entry ce on vv.grid_cell_id = ce.target_grid_cell_id and ce.crosswalk_id = _crosswalk_dataset_id
		where (
		  ve.variable_dataset_id = _dataset_id
		  and (_variable_name is null or ve."name" = any(_variable_name))
		)
		group by
   		  ve."name",
		  ce.source_grid_cell_id
		order by
		  ve.name,
		  ce.source_grid_cell_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
		select
		  ve.name,
		  vv.grid_cell_id,
		  vv.value
		from data.variable_entry ve
		  join data.variable_value vv on vv.variable_entry_id = ve.id
		where (
		  ve.variable_dataset_id = _dataset_id
		  and (_variable_name is null or ve."name" = any(_variable_name))
		)
		order by
			ve.name,
			vv.grid_cell_id;
	end if;	
end
$function$
;

-- DROP SCHEMA grids;

CREATE SCHEMA grids AUTHORIZATION benmap_system;

-- DROP SEQUENCE grids.us_cmaq_12km_nation_clipped_gid_seq;

CREATE SEQUENCE grids.us_cmaq_12km_nation_clipped_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE grids.us_cmaq_12km_nation_gid_seq;

CREATE SEQUENCE grids.us_cmaq_12km_nation_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE grids.us_county_gid_seq;

CREATE SEQUENCE grids.us_county_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE grids.us_nation_gid_seq;

CREATE SEQUENCE grids.us_nation_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE grids.us_state_gid_seq;

CREATE SEQUENCE grids.us_state_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE grids.usa_ct_grids_dissolve_gid_seq;

CREATE SEQUENCE grids.usa_ct_grids_dissolve_gid_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;-- grids.us_cmaq_12km_nation definition

-- Drop table

-- DROP TABLE grids.us_cmaq_12km_nation;

CREATE TABLE grids.us_cmaq_12km_nation (
	gid serial4 NOT NULL,
	area numeric NULL,
	perimeter numeric NULL,
	combo12km_ numeric NULL,
	combo12km1 numeric NULL,
	col numeric NULL,
	"row" numeric NULL,
	newcr numeric NULL,
	euscol numeric NULL,
	eusrow numeric NULL,
	euscr numeric NULL,
	wuscol numeric NULL,
	wusrow numeric NULL,
	wuscr numeric NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT us_cmaq_12km_nation_pkey PRIMARY KEY (gid)
);
CREATE INDEX us_cmaq_12km_nation_geom_idx ON grids.us_cmaq_12km_nation USING gist (geom);


-- grids.us_cmaq_12km_nation_clipped definition

-- Drop table

-- DROP TABLE grids.us_cmaq_12km_nation_clipped;

CREATE TABLE grids.us_cmaq_12km_nation_clipped (
	gid serial4 NOT NULL,
	area numeric NULL,
	perimeter numeric NULL,
	combo12km_ numeric NULL,
	combo12km1 numeric NULL,
	col numeric NULL,
	"row" numeric NULL,
	newcr numeric NULL,
	euscol numeric NULL,
	eusrow numeric NULL,
	euscr numeric NULL,
	wuscol numeric NULL,
	wusrow numeric NULL,
	wuscr numeric NULL,
	oid_ int4 NULL,
	row_1 float8 NULL,
	"column" float8 NULL,
	newcr_1 float8 NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT us_cmaq_12km_nation_clipped_pkey PRIMARY KEY (gid)
);
CREATE INDEX us_cmaq_12km_nation_clipped_geom_idx ON grids.us_cmaq_12km_nation_clipped USING gist (geom);


-- grids.us_county definition

-- Drop table

-- DROP TABLE grids.us_county;

CREATE TABLE grids.us_county (
	gid serial4 NOT NULL,
	"name" varchar(90) NULL,
	state_name varchar(25) NULL,
	state_fips varchar(2) NULL,
	cnty_fips varchar(3) NULL,
	fips varchar(5) NULL,
	col int2 NULL,
	"row" int2 NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT us_county_pkey PRIMARY KEY (gid)
);
CREATE INDEX us_county_geom_idx ON grids.us_county USING gist (geom);


-- grids.us_nation definition

-- Drop table

-- DROP TABLE grids.us_nation;

CREATE TABLE grids.us_nation (
	gid serial4 NOT NULL,
	col int2 NULL,
	"row" int2 NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT us_nation_pkey PRIMARY KEY (gid)
);
CREATE INDEX us_nation_geom_idx ON grids.us_nation USING gist (geom);


-- grids.us_state definition

-- Drop table

-- DROP TABLE grids.us_state;

CREATE TABLE grids.us_state (
	gid serial4 NOT NULL,
	state_name varchar(25) NULL,
	state_fips varchar(2) NULL,
	col int2 NULL,
	"row" int2 NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT us_state_pkey PRIMARY KEY (gid)
);
CREATE INDEX us_state_geom_idx ON grids.us_state USING gist (geom);


-- grids.usa_ct_grids_dissolve definition

-- Drop table

-- DROP TABLE grids.usa_ct_grids_dissolve;

CREATE TABLE grids.usa_ct_grids_dissolve (
	gid serial4 NOT NULL,
	"row" numeric NULL,
	col numeric NULL,
	geom geometry(multipolygon, 4269) NULL,
	CONSTRAINT usa_ct_grids_dissolve_pkey PRIMARY KEY (gid)
);
CREATE INDEX usa_ct_grids_dissolve_geom_idx ON grids.usa_ct_grids_dissolve USING gist (geom);
