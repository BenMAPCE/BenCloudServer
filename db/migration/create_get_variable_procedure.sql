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
