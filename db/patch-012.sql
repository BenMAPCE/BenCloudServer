-- Adding support for batch tasks

UPDATE "data".settings SET value_int=12 where "key"='version';

CREATE TABLE "data".task_batch (
	id serial4 NOT NULL,
	"name" text NULL,
	parameters text NULL,
	user_id text NULL,
	sharing_scope int2 NULL DEFAULT 0,
	CONSTRAINT task_batch_pk PRIMARY KEY (id)
);

ALTER TABLE "data".task_complete ADD task_batch_id int4 NOT null default 0;
ALTER TABLE "data".task_queue ADD task_batch_id int4 NOT null default 0;

ALTER TABLE "data".hif_result_function_config ADD hif_instance_id int4;
ALTER TABLE "data".hif_result ADD hif_instance_id int4;

ALTER TABLE "data".valuation_result_function_config ADD hif_instance_id int4, ADD vf_instance_id int4;
ALTER TABLE "data".valuation_result ADD hif_instance_id int4, ADD vf_instance_id int4;

-- Removing 12km trimmed from the list of grids for now
DELETE FROM "data".grid_definition WHERE id=27;