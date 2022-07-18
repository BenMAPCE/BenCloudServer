CREATE OR REPLACE FUNCTION data.get_incidence(_dataset_id integer, _year integer, _endpoint_id integer, _race_id integer[], _ethnicity_id integer[], _gender_id integer[], _start_age smallint, _end_age smallint, _group_by_race boolean, _group_by_ethnicity boolean, _group_by_gender boolean, _group_by_age_range boolean, _output_grid_definition_id integer)
 RETURNS TABLE(grid_cell_id int8, race_id integer, ethnicity_id integer, gender_id integer, start_age smallint, end_age smallint, value double precision)
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
