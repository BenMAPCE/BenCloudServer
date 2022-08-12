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

	 
CREATE TABLE "data".health_impact_function_group_member
(
  health_impact_function_group_id INT
, health_impact_function_id INT
);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 886);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 862);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 882);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 883);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 884);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 885);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 875);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 876);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 878);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 877);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 874);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 903);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 904);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 905);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 906);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 913);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 914);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 907);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 908);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 909);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 910);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 911);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 912);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 915);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 916);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 872);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 873);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 880);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 860);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 917);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 918);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 869);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 870);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 879);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 960);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 967);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 980);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 956);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 970);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 971);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 973);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 972);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1002);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1001);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1003);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1004);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1007);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1006);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1008);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1009);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1005);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 1010);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 988);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 981);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 989);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 982);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 983);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 984);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 990);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 974);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 958);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 985);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 957);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(3, 961);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 886);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 862);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 882);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 883);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 884);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 885);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 875);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 876);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 878);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 877);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 874);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 880);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 860);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 879);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 960);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 967);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 956);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 970);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 971);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 973);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 972);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 974);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 958);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 957);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(6, 961);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 893);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 888);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 889);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 887);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 871);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 931);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 932);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 933);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 934);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 935);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 936);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 937);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 938);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 939);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 940);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 941);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 942);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 943);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 944);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 945);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 946);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 947);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 948);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 949);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 950);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 951);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 952);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 953);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 954);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 955);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 959);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 975);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 964);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 963);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 962);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 966);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 986);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 987);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 968);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 969);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 976);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 965);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 892);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 899);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 865);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 900);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 866);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 919);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 929);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 930);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 920);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 896);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 921);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 922);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 923);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 924);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 925);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 926);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 927);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 928);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 901);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 902);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 867);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 868);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 894);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 863);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 895);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 864);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 881);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 861);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 897);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 898);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 890);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 891);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 999);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 998);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 997);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1000);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 996);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 991);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 994);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 995);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 979);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 992);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 993);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 978);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 977);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 881);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 861);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 890);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 891);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 979);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 978);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 977);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(2, 1018);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 1012);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1012);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1014);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1015);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1013);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1016);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1017);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(5, 1011);
INSERT INTO data.health_impact_function_group_member (health_impact_function_group_id, health_impact_function_id) VALUES(1, 1011);

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
  "name" text NOT NULL
);

CREATE TABLE "data".variable_entry
(
  "id" SERIAL PRIMARY KEY NOT NULL,
  variable_dataset_id INTEGER,
  "name" text NOT NULL,
  grid_definition_id INTEGER
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
