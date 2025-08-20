/**** Add security columns to health impact and valuation function tables ****/


UPDATE data.settings SET value_int=68 WHERE "key"='version';

ALTER TABLE "data".health_impact_function ADD user_id text NULL;
ALTER TABLE "data".health_impact_function ADD share_scope smallint NULL DEFAULT 0;
UPDATE "data".health_impact_function set share_scope = 1;

ALTER TABLE "data".health_impact_function_group ADD user_id text NULL;
ALTER TABLE "data".health_impact_function_group ADD share_scope smallint NULL DEFAULT 0;
UPDATE "data".health_impact_function_group set share_scope = 1;

ALTER TABLE "data".valuation_function ADD user_id text NULL;
ALTER TABLE "data".valuation_function ADD share_scope smallint NULL DEFAULT 0;
UPDATE "data".valuation_function set share_scope = 1;

ALTER TABLE "data".endpoint_group ADD user_id text NULL;
ALTER TABLE "data".endpoint_group ADD share_scope smallint NULL DEFAULT 0;
UPDATE "data".endpoint_group set share_scope = 1;


/**** Add timing field, to replace seasonal metric and metric statistic ****/

ALTER TABLE "data".health_impact_function ADD timing_id int4 NULL;
ALTER TABLE "data".hif_result_function_config ADD timing_id int4 NULL;
ALTER TABLE "data".exposure_result_function_config ADD timing_id int4 NULL;

CREATE TABLE "data".timing_type (
id serial4 NOT NULL,
"name" text NOT NULL,
CONSTRAINT timing_type_pkey PRIMARY KEY (id)
);

INSERT INTO "data".timing_type (id, "name") VALUES (1, 'Annual');
INSERT INTO "data".timing_type (id, "name")	VALUES (2, 'Daily');

update data.health_impact_function set timing_id = 1 where metric_statistic = 1;
update data.health_impact_function set timing_id = 2 where metric_statistic = 0;

INSERT INTO "data".health_impact_function_dataset (id, "name")	VALUES (1, 'User-uploaded functions');