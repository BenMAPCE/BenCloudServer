CREATE OR REPLACE FUNCTION data.get_population(_dataset_id integer, _year integer, _race_id integer[], _ethnicity_id integer[], _gender_id integer[], _age_range_id integer[], _group_by_race boolean, _group_by_ethnicity boolean, _group_by_gender boolean, _group_by_age_range boolean, _output_grid_definition_id integer)
 RETURNS TABLE(grid_cell_id int8, race_id integer, ethnicity_id integer, gender_id integer, age_range_id integer, pop_value double precision)
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
