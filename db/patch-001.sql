-- The 2021-12-10 database backup was loaded to our EPA AWS dev instance to create the database
-- ALL design or data changes to the benmap database made after that backup will be applied by sql patch scripts
-- This is the first such script. As part of this update, we will be storing the database version as an integer 
-- in the new settings table. By sychronizing this version number with the patch

CREATE TABLE "data".settings (
	"key" text NULL,
	value_text text NULL,
	value_int integer NULL
);

INSERT INTO "data".settings ("key", value_text, value_int) VALUES('version', null, 1);

ALTER TABLE "data".hif_result_dataset ADD task_log json NULL;
ALTER TABLE "data".valuation_result_dataset ADD task_log json NULL;

CREATE TABLE "data".task_config (
	id serial4 NOT NULL,
	"name" text NULL,
	"type" text null,
	parameters json NULL,
	CONSTRAINT task_config_dataset_pkey PRIMARY KEY (id)
);