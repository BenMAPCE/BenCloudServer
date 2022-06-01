CREATE OR REPLACE FUNCTION data.get_valuation_results(_dataset_id integer, _hif_id integer[], _vf_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, hif_id integer, vf_id integer, point_estimate double precision, mean double precision, standard_dev double precision, variance double precision, pct_2_5 double precision, pct_97_5 double precision)
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
	select aql.grid_definition_id from data.valuation_result_dataset vrd join data.hif_result_dataset hrd on vrd.hif_result_dataset_id = hrd.id join data.air_quality_layer aql on aql.id = hrd.baseline_aq_layer_id where vrd.id = _dataset_id LIMIT 1
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
       			vr.hif_id,
       			vr.vf_id,
       			sum(vr."result" * ce.percentage) as point_estimate,
       			sum(vr.result_mean * ce.percentage) as mean,
				sum(vr.standard_dev * ce.percentage) as standard_dev,
       			sum(vr.result_variance * ce.percentage) as "variance",
       			sum(vr.pct_2_5 * ce.percentage) as pct_2_5,
       			sum(vr.pct_97_5 * ce.percentage) as pct_97_5 
  			FROM 
       			data.valuation_result vr 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = vr.grid_cell_id					
 			WHERE
				vr.valuation_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR vr.hif_id = ANY(_hif_id)) and
				(_vf_id IS NULL OR vr.hif_id = ANY(_vf_id))
			GROUP BY
				ce.target_col, 
       			ce.target_row,
       			vr.hif_id,
       			vr.vf_id 
			ORDER BY
				ce.target_col, 
       			ce.target_row,
       			vr.hif_id,
       			vr.vf_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			SELECT 
       			vr.grid_col, 
       			vr.grid_row,
       			vr.hif_id,
       			vr.vf_id,
       			vr."result" as point_estimate,
       			vr.result_mean as mean,
				vr.standard_dev as standard_dev,
       			vr.result_variance as "variance",
       			vr.pct_2_5 as pct_2_5,
       			vr.pct_97_5 as pct_97_5 
  			FROM 
       			data.valuation_result vr 					
 			WHERE
				vr.valuation_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR vr.hif_id = ANY(_hif_id)) and
				(_vf_id IS NULL OR vr.hif_id = ANY(_vf_id))
			ORDER BY
       			vr.grid_col, 
       			vr.grid_row,
       			vr.hif_id,
       			vr.vf_id;
	end if;	
end
$function$
;
