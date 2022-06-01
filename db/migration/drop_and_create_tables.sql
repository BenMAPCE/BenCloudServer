DROP SCHEMA IF EXISTS data CASCADE;
CREATE SCHEMA data;


SET search_path = data;

CREATE TABLE "data".settings (
	"key" text NULL,
	value_text text NULL,
	value_int integer NULL
);

INSERT INTO "data".settings ("key", value_text, value_int) VALUES('version', null, 2);

CREATE TABLE "data".task_config (
	id serial NOT NULL,
	"name" text NULL,
	"type" text null,
	parameters json NULL,
	CONSTRAINT task_config_dataset_pkey PRIMARY KEY (id)
);

CREATE TABLE data.task_queue (
    task_id SERIAL PRIMARY KEY NOT NULL,
    task_user_identifier TEXT,
    task_priority INTEGER,
    task_uuid TEXT,
	task_parent_uuid TEXT,
    task_name TEXT,
    task_description TEXT,
    task_type TEXT,
    task_parameters TEXT,
 	task_percentage INTEGER,
    task_message TEXT,
    task_in_process BOOLEAN DEFAULT false,
	task_submitted_date TIMESTAMP NULL,
	task_started_date TIMESTAMP NULL
);

CREATE INDEX task_queue_on_date ON data.task_queue (task_submitted_date);
CREATE INDEX task_queue_on_priority_submitted_date ON data.task_queue (task_priority, task_submitted_date);
CREATE INDEX task_queue_on_uuid ON data.task_queue (task_uuid);

CREATE TABLE data.task_complete (
    task_id SERIAL PRIMARY KEY NOT NULL,
    task_uuid TEXT,
	task_parent_uuid TEXT,
    task_user_identifier TEXT,
    task_priority INTEGER,
    task_name TEXT,
    task_description TEXT,
    task_type TEXT,
    task_parameters TEXT,
    task_results TEXT,
    task_successful BOOLEAN,
    task_complete_message TEXT,
    task_submitted_date TIMESTAMP NULL,
	task_started_date TIMESTAMP NULL,
	task_completed_date TIMESTAMP NULL
);

CREATE INDEX task_complete_on_date ON data.task_complete (task_completed_date);
CREATE INDEX task_complete_on_uuid ON data.task_complete (task_uuid);

CREATE TABLE data.task_worker (
  task_id SERIAL PRIMARY KEY NOT NULL,
  task_worker_uuid TEXT,
  task_uuid TEXT,
  last_heartbeat_date TIMESTAMP NULL
);

CREATE INDEX task_worker_on_uuid ON data.task_worker (task_worker_uuid);
CREATE INDEX task_worker_on_task_uuid ON data.task_worker (task_uuid);

CREATE TABLE data.hif_result_dataset (
    id SERIAL PRIMARY KEY NOT NULL,
    task_uuid TEXT,
	name TEXT,
	population_dataset_id INTEGER,
	population_year INTEGER,
	baseline_aq_layer_id INTEGER,
	scenario_aq_layer_id INTEGER,
	task_log json NULL
);

CREATE TABLE data.hif_result_function_config (
  hif_result_dataset_id INTEGER
, hif_id INTEGER
, start_age INT
, end_age INT
, incidence_dataset_id INT
, prevalence_dataset_id INT
, variable_dataset_id INT
, race_id INTEGER
, gender_id INTEGER
, ethnicity_id INTEGER
, metric_id INTEGER
, seasonal_metric_id INTEGER
, metric_statistic INTEGER
);

CREATE TABLE data.hif_result (
    hif_result_dataset_id INTEGER,
    hif_id INTEGER,
    grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population FLOAT8,
	delta_aq FLOAT8,
	baseline_aq FLOAT8,
	scenario_aq FLOAT8,
	incidence FLOAT8,
    result FLOAT8,
	baseline FLOAT8,
	result_mean FLOAT8,
	standard_dev FLOAT8,
	result_variance FLOAT8,
	pct_2_5 FLOAT8,
	pct_97_5 FLOAT8,
	percentiles FLOAT8[]
);

CREATE TABLE "data".valuation_result_dataset (
	id serial NOT NULL,
	task_uuid text NULL,
	hif_result_dataset_id INTEGER,
	name TEXT,
	variable_dataset_id INT,
	task_log json NULL,
	CONSTRAINT valuation_result_dataset_pkey PRIMARY KEY (id)
);

CREATE TABLE "data".valuation_result_function_config (
	valuation_result_dataset_id int4 NULL,
	vf_id int4 NULL,
	hif_id int4 NULL
);

CREATE TABLE "data".valuation_result (
	valuation_result_dataset_id int4 NULL,
	vf_id int4 NULL,
	hif_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population FLOAT8 NULL,
	"result" FLOAT8 NULL,
	result_mean FLOAT8,
	standard_dev FLOAT8,
	result_variance FLOAT8,
	pct_2_5 FLOAT8,
	pct_97_5 FLOAT8,
	percentiles FLOAT8[]
);


CREATE TABLE "air_quality_layer" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text,
  "pollutant_id" int4,
  "grid_definition_id" int4,
  "locked" boolean default false
);

CREATE TABLE "air_quality_layer_metrics" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "air_quality_layer_id" int4,
  "metric_id" int4,
  "seasonal_metric_id" int4,
  "annual_statistic_id" int4,
  "cell_count" int4,
  "min_value" FLOAT8,
  "max_value" FLOAT8,
  "mean_value" FLOAT8,
  "pct_2_5" FLOAT8,
  "pct_97_5" FLOAT8,
  "cell_count_above_lrl" int4
);

CREATE TABLE "air_quality_cell" (
  "air_quality_layer_id" int4,
  "grid_col" int,
  "grid_row" int,
  "grid_cell_id" int8,
  "metric_id" int4,
  "seasonal_metric_id" int4,
  "annual_statistic_id" int4,
  "value" FLOAT8
);

CREATE TABLE "data".health_impact_function_group
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL,
  "help_text" text
);
INSERT INTO "data".health_impact_function_group (id,"name",help_text) VALUES
	 (1,'Premature Death - All','Depending on the pollutant, this may include both all-cause and cause-specific mortality risks, e.g., respiratory mortality, cardiovascular mortality, and lung cancer.'),
	 (2,'Chronic Effects - All','This includes health effects with longer-term impacts such as new asthma diagnoses, non-fatal lung cancer, stroke, and myocardial infarction.'),
	 (3,'Acute Effects - All','This includes shorter-term health impacts such as asthma exacerbations and emergency department visits or hospital admissions for cardiovascular or respiratory disease.'),
	 (5,'Premature Death - Primary','Primary functions selected from the larger premature death group'),
	 (6,'Acute Effects - Primary','Primary functions selected from the larger acute effects group'),
	 (7,'Ozone Transport Functions','Created to support testing and validation');
	 
CREATE TABLE "data".health_impact_function_group_member
(
  health_impact_function_group_id INT
, health_impact_function_id INT
);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,860),
	 (6,860),
	 (1,861),
	 (5,861),
	 (3,862),
	 (6,862),
	 (1,863),
	 (1,864),
	 (1,865),
	 (1,866);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,867),
	 (1,868),
	 (3,869),
	 (3,870),
	 (2,871),
	 (3,872),
	 (3,873),
	 (3,874),
	 (6,874),
	 (3,875);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (6,875),
	 (3,876),
	 (6,876),
	 (3,877),
	 (6,877),
	 (3,878),
	 (6,878),
	 (3,879),
	 (6,879),
	 (3,880);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (6,880),
	 (1,881),
	 (5,881),
	 (3,882),
	 (6,882),
	 (3,883),
	 (6,883),
	 (3,884),
	 (6,884),
	 (3,885);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (6,885),
	 (3,886),
	 (6,886),
	 (2,887),
	 (2,888),
	 (2,889),
	 (1,890),
	 (5,890),
	 (1,891),
	 (5,891);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,892),
	 (2,893),
	 (1,894),
	 (1,895),
	 (1,896),
	 (1,897),
	 (1,898),
	 (1,899),
	 (1,900),
	 (1,901);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,902),
	 (3,903),
	 (3,904),
	 (3,905),
	 (3,906),
	 (3,907),
	 (3,908),
	 (3,909),
	 (3,910),
	 (3,911);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,912),
	 (3,913),
	 (3,914),
	 (3,915),
	 (3,916),
	 (3,917),
	 (3,918),
	 (1,919),
	 (1,920),
	 (1,921);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,922),
	 (1,923),
	 (1,924),
	 (1,925),
	 (1,926),
	 (1,927),
	 (1,928),
	 (1,929),
	 (1,930),
	 (2,931);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (2,932),
	 (2,933),
	 (2,934),
	 (2,935),
	 (2,936),
	 (2,937),
	 (2,938),
	 (2,939),
	 (2,940),
	 (2,941);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (2,942),
	 (2,943),
	 (2,944),
	 (2,945),
	 (2,946),
	 (2,947),
	 (2,948),
	 (2,949),
	 (2,950),
	 (2,951);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (2,952),
	 (2,953),
	 (2,954),
	 (2,955),
	 (3,956),
	 (6,956),
	 (3,957),
	 (6,957),
	 (3,958),
	 (6,958);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (2,959),
	 (3,960),
	 (6,960),
	 (3,961),
	 (6,961),
	 (2,962),
	 (2,963),
	 (2,964),
	 (2,965),
	 (2,966);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,967),
	 (6,967),
	 (2,968),
	 (2,969),
	 (3,970),
	 (6,970),
	 (3,971),
	 (6,971),
	 (3,972),
	 (6,972);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,973),
	 (6,973),
	 (3,974),
	 (6,974),
	 (2,975),
	 (2,976),
	 (1,977),
	 (5,977),
	 (1,978),
	 (5,978);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,979),
	 (5,979),
	 (3,980),
	 (3,981),
	 (3,982),
	 (3,983),
	 (3,984),
	 (3,985),
	 (2,986),
	 (2,987);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,988),
	 (3,989),
	 (3,990),
	 (1,991),
	 (1,992),
	 (1,993),
	 (1,994),
	 (1,995),
	 (1,996),
	 (1,997);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (1,998),
	 (1,999),
	 (1,1000),
	 (3,1001),
	 (3,1002),
	 (3,1003),
	 (3,1004),
	 (3,1005),
	 (3,1006),
	 (3,1007);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (3,1008),
	 (3,1009),
	 (3,1010),
	 (7,874),
	 (7,875),
	 (7,876),
	 (7,877),
	 (7,878),
	 (7,879),
	 (7,880);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (7,881),
	 (7,882),
	 (7,883),
	 (7,884),
	 (7,885),
	 (7,886),
	 (7,887),
	 (7,888),
	 (7,889),
	 (7,890);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id) VALUES
	 (7,891);

CREATE TABLE "statistic_type" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);
INSERT INTO "data".statistic_type(id, "name")VALUES(0, 'None');
INSERT INTO "data".statistic_type(id, "name")VALUES(1, 'Mean');

-------------------------------------------------

CREATE TABLE "age_range" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "pop_config_id" int4,
  "name" text NOT NULL,
  "start_age" int2,
  "end_age" int2
);

CREATE TABLE "endpoint_group" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "ethnicity" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text
);

CREATE TABLE "gender" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text
);

CREATE TABLE "grid_definition" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text,
  "col_count" int4,
  "row_count" int4,
  "is_admin_layer" varchar(1),
  "draw_priority" int4,
  "outline_color" varchar(50),
  "table_name" text
);

CREATE TABLE "data".crosswalk_dataset
(
  "id" SERIAL PRIMARY KEY NOT NULL
, source_grid_id INTEGER
, target_grid_id INTEGER
);

CREATE TABLE "data".crosswalk_entry
(
  crosswalk_id int4
, source_col int4
, source_row int4
, source_grid_cell_id int8
, target_col int4
, target_row int4
, target_grid_cell_id int8
, percentage FLOAT8
)
;

CREATE TABLE "incidence_dataset" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL,
  "grid_definition_id" int4
);

CREATE TABLE "incidence_entry" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "incidence_dataset_id" int4,
  "year" int4,
  "endpoint_group_id" int4,
  "endpoint_id" int4,
  "race_id" int4,
  "gender_id" int4,
  "start_age" int2,
  "end_age" int2,
  "prevalence" boolean,
  "ethnicity_id" int4
);

CREATE TABLE "incidence_value" (
  "incidence_entry_id" int4,
  "grid_cell_id" int8,
  "grid_col" int4,
  "grid_row" int4,
  "value" FLOAT8
);

CREATE TABLE "income_growth_adj_dataset" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "income_growth_adj_factor" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "income_growth_adj_dataset_id" int2 NOT NULL,
  "growth_year" int2 NOT NULL,
  "mean_value" FLOAT8 NOT NULL,
  "endpoint_group_id" int2 NOT NULL
);

CREATE TABLE "pollutant" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text,
  "observation_type" int2,
  "friendly_name" text,
  "help_text" text
);

CREATE TABLE "pollutant_metric" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "pollutant_id" int4,
  "name" text,
  "hourly_metric_generation" int2,
  "window_size" int2,
  "window_statistic" int2,
  "start_hour" int2,
  "end_hour" int2,
  "daily_statistic" int2
);

CREATE TABLE "pop_config" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "pop_config_ethnicity" (
  "pop_config_id" int4,
  "ethnicity_id" int4
);

CREATE TABLE "pop_config_gender" (
  "pop_config_id" int4,
  "gender_id" int4
);

CREATE TABLE "pop_config_race" (
  "pop_config_id" int4,
  "race_id" int4
);

CREATE TABLE "population_dataset" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text,
  "pop_config_id" int4,
  "grid_definition_id" int4,
  "apply_growth" int4
);

CREATE TABLE "population_entry" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "pop_dataset_id" int4,
  "race_id" int4,
  "ethnicity_id" int4,
  "gender_id" int4,
  "age_range_id" int4,
  "pop_year" int2
);

CREATE TABLE "population_value" (
  "pop_entry_id" int4,
  "grid_cell_id" int8,
  "pop_value" FLOAT8
);

CREATE TABLE "race" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text
);

CREATE TABLE "seasonal_metric" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "metric_id" int4,
  "name" text
);

CREATE TABLE "seasonal_metric_season" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "seasonal_metric_id" int4,
  "start_day" int2,
  "end_day" int2,
  "seasonal_metric_type" int2,
  "metric_function" text
);

CREATE TABLE "endpoint" (
  "id" SERIAL PRIMARY KEY NOT NULL,
  "endpoint_group_id" int2 NOT NULL,
  "name" text NOT NULL
);


CREATE TABLE "data".health_impact_function_dataset
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "data".health_impact_function
(
  "id" SERIAL PRIMARY KEY NOT NULL
, health_impact_function_dataset_id INT
, endpoint_group_id INT
, endpoint_id INT
, pollutant_id INT
, metric_id INT
, seasonal_metric_id INT
, metric_statistic INT
, author TEXT
, function_year INT
, location TEXT
, other_pollutants TEXT
, qualifier TEXT
, reference TEXT
, start_age INT
, end_age INT
, function_text TEXT
, incidence_dataset_id INT
, prevalence_dataset_id INT
, variable_dataset_id INT
, beta FLOAT8
, dist_beta TEXT
, p1_beta FLOAT8
, p2_beta FLOAT8
, val_a FLOAT8
, name_a TEXT
, val_b FLOAT8
, name_b TEXT
, val_c FLOAT8
, name_c TEXT
, baseline_function_text TEXT
, race_id INTEGER
, gender_id INTEGER
, ethnicity_id INTEGER
, start_day integer
, end_day integer
);

CREATE TABLE "data".valuation_function_dataset
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "data".valuation_function
(
 "id" SERIAL PRIMARY KEY NOT NULL
, valuation_dataset_id INT
, endpoint_group_id INT
, endpoint_id INT
, qualifier TEXT
, reference TEXT
, start_age INT
, end_age INT
, function_text TEXT
, val_a FLOAT8
, name_a TEXT
, dist_a TEXT
, p1a FLOAT8
, p2a FLOAT8
, val_b FLOAT8
, name_b TEXT
, val_c FLOAT8
, name_c TEXT
, val_d FLOAT8
, name_d TEXT
);

CREATE TABLE "data".variable_dataset
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL,
  grid_definition_id INTEGER
);

CREATE TABLE "data".variable_entry
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  variable_dataset_id INTEGER,
  "name" text NOT NULL
);

CREATE TABLE "data".variable_value
(
  variable_entry_id INTEGER
, grid_col INTEGER
, grid_row INTEGER
, "value" FLOAT8
, grid_cell_id int8
);

CREATE TABLE "data".inflation_dataset
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  "name" text NOT NULL
);

CREATE TABLE "data".inflation_entry
(
  inflation_dataset_id INTEGER
, entry_year INTEGER
, all_goods_index FLOAT8
, medical_cost_index FLOAT8
, wage_index FLOAT8
);


ALTER TABLE "endpoint" ADD FOREIGN KEY ("endpoint_group_id") REFERENCES "endpoint_group" ("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "incidence_entry" ADD FOREIGN KEY ("incidence_dataset_id") REFERENCES "incidence_dataset" ("id");

ALTER TABLE "pollutant_metric" ADD FOREIGN KEY ("pollutant_id") REFERENCES "pollutant" ("id");

ALTER TABLE "seasonal_metric" ADD FOREIGN KEY ("metric_id") REFERENCES "pollutant_metric" ("id");

ALTER TABLE "pop_config_race" ADD FOREIGN KEY ("race_id") REFERENCES "race" ("id");

ALTER TABLE "population_entry" ADD FOREIGN KEY ("pop_dataset_id") REFERENCES "population_dataset" ("id");

ALTER TABLE "population_dataset" ADD FOREIGN KEY ("pop_config_id") REFERENCES "pop_config" ("id");

ALTER TABLE "pop_config_race" ADD FOREIGN KEY ("pop_config_id") REFERENCES "pop_config" ("id");

ALTER TABLE "pop_config_gender" ADD FOREIGN KEY ("pop_config_id") REFERENCES "pop_config" ("id");

ALTER TABLE "pop_config_gender" ADD FOREIGN KEY ("gender_id") REFERENCES "gender" ("id");

ALTER TABLE "age_range" ADD FOREIGN KEY ("pop_config_id") REFERENCES "pop_config" ("id");

ALTER TABLE "pop_config_ethnicity" ADD FOREIGN KEY ("pop_config_id") REFERENCES "pop_config" ("id");

ALTER TABLE "pop_config_ethnicity" ADD FOREIGN KEY ("ethnicity_id") REFERENCES "ethnicity" ("id");

ALTER TABLE "income_growth_adj_factor" ADD FOREIGN KEY ("income_growth_adj_dataset_id") REFERENCES "income_growth_adj_dataset" ("id");

ALTER TABLE "income_growth_adj_factor" ADD FOREIGN KEY ("endpoint_group_id") REFERENCES "endpoint_group" ("id");

ALTER TABLE "incidence_dataset" ADD FOREIGN KEY ("grid_definition_id") REFERENCES "grid_definition" ("id");

ALTER TABLE "incidence_value" ADD FOREIGN KEY ("incidence_entry_id") REFERENCES "incidence_entry" ("id");

ALTER TABLE "seasonal_metric_season" ADD FOREIGN KEY ("seasonal_metric_id") REFERENCES "seasonal_metric" ("id");

ALTER TABLE "air_quality_layer" ADD FOREIGN KEY ("pollutant_id") REFERENCES "pollutant" ("id");

ALTER TABLE "air_quality_layer" ADD FOREIGN KEY ("grid_definition_id") REFERENCES "grid_definition" ("id");

ALTER TABLE "air_quality_cell" ADD FOREIGN KEY ("air_quality_layer_id") REFERENCES "air_quality_layer" ("id");

ALTER TABLE "air_quality_cell" ADD FOREIGN KEY ("metric_id") REFERENCES "pollutant_metric" ("id");

ALTER TABLE "air_quality_cell" ADD FOREIGN KEY ("seasonal_metric_id") REFERENCES "seasonal_metric" ("id");
