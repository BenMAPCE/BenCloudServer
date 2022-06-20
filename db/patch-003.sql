-- Fixes structure of variable datasets so each variable can be tied to a grid definition, rather than doing that at the dataset level

UPDATE "data".settings SET value_int=3 where "key"='version';

-- Add security fields
ALTER TABLE "data".air_quality_layer ADD user_id text NULL;
ALTER TABLE "data".air_quality_layer DROP COLUMN "locked";
ALTER TABLE "data".air_quality_layer ADD share_scope smallint NULL DEFAULT 0;
update "data".air_quality_layer set share_scope = 1;

ALTER TABLE "data".hif_result_dataset ADD user_id text NULL;
ALTER TABLE "data".hif_result_dataset ADD sharing_scope smallint NULL DEFAULT 0;

ALTER TABLE "data".valuation_result_dataset ADD user_id text NULL;
ALTER TABLE "data".valuation_result_dataset ADD sharing_scope smallint NULL DEFAULT 0;

ALTER TABLE "data".task_queue DROP COLUMN task_user_identifier;
ALTER TABLE "data".task_queue ADD user_id text NULL;
ALTER TABLE "data".task_queue ADD sharing_scope smallint NULL DEFAULT 0;

ALTER TABLE "data".task_complete DROP COLUMN task_user_identifier;
ALTER TABLE "data".task_complete ADD user_id text NULL;
ALTER TABLE "data".task_complete ADD sharing_scope smallint NULL DEFAULT 0;

ALTER TABLE "data".task_config ADD user_id text NULL;
ALTER TABLE "data".task_config ADD sharing_scope smallint NULL DEFAULT 0;

-- Fix variable to grid relationship
DROP TABLE "data".variable_dataset;
DROP TABLE "data".variable_entry;

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

INSERT INTO "data".variable_dataset (id,"name") VALUES
	 (1,'EPA Standard Variables');

INSERT INTO "data".variable_entry (id,variable_dataset_id,"name",grid_definition_id) VALUES
	 (89,1,'below_poverty_line',18),
	 (90,1,'above_poverty_line',18),
	 (91,1,'below_2x_poverty_line',18),
	 (92,1,'above_2x_poverty_line',18),
	 (93,1,'no_hs_degree',18),
	 (94,1,'hs_degree_plus',18),
	 (95,1,'above_2x_poverty_line_tract',33),
	 (96,1,'above_poverty_line_tract',33),
	 (97,1,'below_2x_poverty_line_tract',33),
	 (98,1,'below_poverty_line_tract',33);
INSERT INTO "data".variable_entry (id,variable_dataset_id,"name",grid_definition_id) VALUES
	 (99,1,'english_less_than_verywell',18),
	 (100,1,'english_less_than_verywell_tract',33),
	 (101,1,'english_less_than_well',18),
	 (102,1,'english_less_than_well_tract',33),
	 (103,1,'english_verywell_or_better',18),
	 (104,1,'english_verywell_or_better_tract',33),
	 (105,1,'english_well_or_better',18),
	 (106,1,'english_well_or_better_tract',33),
	 (107,1,'hs_degree_plus_tract',33),
	 (108,1,'no_hs_degree_tract',33);
INSERT INTO "data".variable_entry (id,variable_dataset_id,"name",grid_definition_id) VALUES
	 (67,1,'averagehhsize',18),
	 (65,1,'median_income',18),
	 (66,1,'natl_median_income',18),
	 (83,1,'unemployment_rate_2017_18',18),
	 (84,1,'pct_BlueCollar',18),
	 (85,1,'pct_uninsured1_17',18),
	 (86,1,'pct_uninsured18_39',18),
	 (87,1,'pct_uninsured40_64',18),
	 (88,1,'pct_uninsuredUnder_65',18);

-- TODO: Drop and create the get_variable procedure
DROP FUNCTION data.get_variable;

CREATE OR REPLACE FUNCTION data.get_variable(_dataset_id integer, _variable_name text, _output_grid_definition_id integer)
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
	SELECT grid_definition_id FROM data.variable_entry WHERE variable_dataset_id = _dataset_id and name = _variable_name LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Value not found in variable_entry';
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
		  and ve."name" = _variable_name
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
		  and ve."name" = _variable_name
		)
		order by
			ve.name,
			vv.grid_cell_id;
	end if;	
end
$function$
;

-- Reset the sequences to make sure nothing is out of whack
SELECT SETVAL('data.variable_dataset_id_seq', COALESCE(MAX(id), 1) ) FROM data.variable_dataset;
SELECT SETVAL('data.variable_entry_id_seq', COALESCE(MAX(id), 1) ) FROM data.variable_entry;