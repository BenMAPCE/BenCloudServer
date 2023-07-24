-- Adding exposure result field
-- Adding get_exposure_results function

UPDATE "data".settings SET value_int=16 where "key"='version';

ALTER TABLE "data".exposure_result ADD "result" float8 NULL;

CREATE OR REPLACE FUNCTION data.get_exposure_results(_dataset_id integer, _ef_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, exposure_function_id integer, result double precision, subgroup_population double precision, all_population double precision, baseline_aq double precision, scenario_aq double precision, delta_aq double precision)
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
	select erd.grid_definition_id from data.exposure_result_dataset erd where erd.id = _dataset_id LIMIT 1
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
       			er.exposure_function_id,
       			sum(er."result" * ce.percentage) as result,
       			sum(er.subgroup_population * ce.percentage) as subgroup_population,
       			sum(er.all_population * ce.percentage) as all_population,
       			sum(er.baseline_aq * ce.percentage * er.subgroup_population) / sum(er.all_population) as baseline_aq,
       			sum(er.scenario_aq * ce.percentage * er.subgroup_population) / sum(er.all_population) as scenario_aq,
       			sum(er.delta_aq * ce.percentage * er.subgroup_population) / sum(er.all_population) as delta_aq
  			FROM 
       			data.exposure_result er 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = er.grid_cell_id					
 			WHERE
				er.exposure_result_dataset_id = _dataset_id and
				(_ef_id IS NULL OR er.exposure_function_id = ANY(_ef_id))
			GROUP BY
				ce.target_col, 
       			ce.target_row,
       			er.exposure_function_id 
			ORDER BY
				ce.target_col, 
       			ce.target_row,
       			er.exposure_function_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			SELECT 
       			er.grid_col, 
       			er.grid_row,
       			er.exposure_function_id,
       			er."result",
       			er.subgroup_population as subgroup_population,
       			er.all_population as all_population,
       			er.baseline_aq,
       			er.scenario_aq,
       			er.delta_aq
   			FROM 
       			data.exposure_result er 					
 			WHERE
				er.exposure_result_dataset_id = _dataset_id and
				(_ef_id IS NULL OR er.exposure_function_id = ANY(_ef_id))
			ORDER BY
       			er.grid_col, 
       			er.grid_row,
       			er.exposure_function_id;
	end if;	
end
$function$
;
