/**** update functions to allow aggregating to the whole study area when target grid definition is 0.****/


UPDATE "data".settings SET value_int=41 WHERE "key"='version';

--Exposure results
-- DROP FUNCTION "data".get_exposure_results(int4, _int4, int4);

CREATE OR REPLACE FUNCTION data.get_exposure_results(_dataset_id integer, _ef_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, exposure_function_id integer, exposure_function_instance_id integer, result double precision, subgroup_population double precision, all_population double precision, baseline_aq double precision, scenario_aq double precision, delta_aq double precision)
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
-- Aggregate to the whole study area
if (_output_grid_definition_id = 0) then
	return query
		SELECT 
   			0 as target_col, 
   			0 as target_row,
   			er.exposure_function_id,
   			er.exposure_function_instance_id,
   			sum(er."result") as result,
   			sum(er.subgroup_population) as subgroup_population,
   			sum(er.all_population) as all_population,
			case when sum(er.subgroup_population) = 0 then 0 else sum(er.baseline_aq * er.subgroup_population) / sum(er.subgroup_population) end as baseline_aq, 
			case when sum(er.subgroup_population) = 0 then 0 else sum(er.scenario_aq * er.subgroup_population) / sum(er.subgroup_population) end as scenario_aq,
			case when sum(er.subgroup_population) = 0 then 0 else sum(er.delta_aq * er.subgroup_population) / sum(er.subgroup_population) end as delta_aq
		FROM 
   			data.exposure_result er 		
		WHERE
			er.exposure_result_dataset_id = _dataset_id and
			(_ef_id IS NULL OR er.exposure_function_id = ANY(_ef_id))
		GROUP BY
   			er.exposure_function_id,
   			er.exposure_function_instance_id
		ORDER BY
   			er.exposure_function_id,
   			er.exposure_function_instance_id;
--Aggregate to the selected target grid
else
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
	       			er.exposure_function_instance_id,
	       			sum(er."result" * ce.percentage) as result,
	       			sum(er.subgroup_population * ce.percentage) as subgroup_population,
	       			sum(er.all_population * ce.percentage) as all_population,
					case when sum(er.subgroup_population * ce.percentage) = 0 then 0 else sum(er.baseline_aq * ce.percentage * er.subgroup_population) / sum(er.subgroup_population * ce.percentage) end as baseline_aq, 
					case when sum(er.subgroup_population * ce.percentage) = 0 then 0 else sum(er.scenario_aq * ce.percentage * er.subgroup_population) / sum(er.subgroup_population * ce.percentage) end as scenario_aq,
					case when sum(er.subgroup_population * ce.percentage) = 0 then 0 else sum(er.delta_aq * ce.percentage * er.subgroup_population) / sum(er.subgroup_population * ce.percentage) end as delta_aq
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
	       			er.exposure_function_id,
	       			er.exposure_function_instance_id
				ORDER BY
					ce.target_col, 
	       			ce.target_row,
	       			er.exposure_function_id,
	       			er.exposure_function_instance_id;	
		else
			-- No crosswalk needed return the data straight from the table
			return query
				SELECT 
	       			er.grid_col, 
	       			er.grid_row,
	       			er.exposure_function_id,
	       			er.exposure_function_instance_id,
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
	       			er.exposure_function_id,
	       			er.exposure_function_instance_id;
		end if;	
end if;


end
$function$
;


--HIF results
CREATE OR REPLACE FUNCTION data.get_hif_results(_dataset_id integer, _hif_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, hif_id integer, hif_instance_id integer, point_estimate double precision, population double precision, baseline_aq double precision, scenario_aq double precision, delta_aq double precision, incidence double precision, mean double precision, baseline double precision, standard_dev double precision, variance double precision, pct_2_5 double precision, pct_97_5 double precision, percentiles double precision[])
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
	select hrd.grid_definition_id from data.hif_result_dataset hrd where hrd.id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Could not determine grid definition for result dataset';
	end if;
-- Aggregate to the whole study area
if (_output_grid_definition_id = 0) then
	return query	
		SELECT 
   			0 as target_col, 
   			0 as target_row,
   			hr.hif_id,
   			hr.hif_instance_id,
   			sum(hr."result") as point_estimate,
   			sum(hr.population) as population,
			case when sum(hr.population) = 0 then 0 else sum(hr.baseline_aq * hr.population) / sum(hr.population) end as baseline_aq, 
			case when sum(hr.population) = 0 then 0 else sum(hr.scenario_aq * hr.population) / sum(hr.population) end as scenario_aq,
			case when sum(hr.population) = 0 then 0 else sum(hr.delta_aq * hr.population) / sum(hr.population) end as delta_aq,
   			sum(hr.incidence) as incidence,
   			sum(hr.result_mean) as mean,
			sum(hr.baseline) as baseline,
			sum(hr.standard_dev) as standard_dev,
   			sum(hr.result_variance) as "variance",
   			sum(hr.pct_2_5) as pct_2_5,
   			sum(hr.pct_97_5) as pct_97_5,
   			ARRAY[
   				sum(hr.percentiles[1]),
   				sum(hr.percentiles[2]),
   				sum(hr.percentiles[3]),
   				sum(hr.percentiles[4]),
   				sum(hr.percentiles[5]),
   				sum(hr.percentiles[6]),
   				sum(hr.percentiles[7]),
   				sum(hr.percentiles[8]),
   				sum(hr.percentiles[9]),
   				sum(hr.percentiles[10]),
   				sum(hr.percentiles[11]),
   				sum(hr.percentiles[12]),
   				sum(hr.percentiles[13]),
   				sum(hr.percentiles[14]),
   				sum(hr.percentiles[15]),
   				sum(hr.percentiles[16]),
   				sum(hr.percentiles[17]),
   				sum(hr.percentiles[18]),
   				sum(hr.percentiles[19]),
   				sum(hr.percentiles[20])
   			] as percentiles
		FROM 
   			data.hif_result hr 					
		WHERE
			hr.hif_result_dataset_id = _dataset_id and
			(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
		GROUP BY
   			hr.hif_id,
   			hr.hif_instance_id
		ORDER BY
   			hr.hif_id,
   			hr.hif_instance_id;
--aggregate to selected target grid definition
else
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
       			hr.hif_id,
       			hr.hif_instance_id,
       			sum(hr."result" * ce.percentage) as point_estimate,
       			sum(hr.population * ce.percentage) as population,
				case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.baseline_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as baseline_aq, 
				case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.scenario_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as scenario_aq,
				case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.delta_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as delta_aq,
       			sum(hr.incidence * ce.percentage) as incidence,
       			sum(hr.result_mean * ce.percentage) as mean,
				sum(hr.baseline * ce.percentage) as baseline,
				sum(hr.standard_dev * ce.percentage) as standard_dev,
       			sum(hr.result_variance * ce.percentage) as "variance",
       			sum(hr.pct_2_5 * ce.percentage) as pct_2_5,
       			sum(hr.pct_97_5 * ce.percentage) as pct_97_5,
       			ARRAY[
       				sum(hr.percentiles[1] * ce.percentage),
       				sum(hr.percentiles[2] * ce.percentage),
       				sum(hr.percentiles[3] * ce.percentage),
       				sum(hr.percentiles[4] * ce.percentage),
       				sum(hr.percentiles[5] * ce.percentage),
       				sum(hr.percentiles[6] * ce.percentage),
       				sum(hr.percentiles[7] * ce.percentage),
       				sum(hr.percentiles[8] * ce.percentage),
       				sum(hr.percentiles[9] * ce.percentage),
       				sum(hr.percentiles[10] * ce.percentage),
       				sum(hr.percentiles[11] * ce.percentage),
       				sum(hr.percentiles[12] * ce.percentage),
       				sum(hr.percentiles[13] * ce.percentage),
       				sum(hr.percentiles[14] * ce.percentage),
       				sum(hr.percentiles[15] * ce.percentage),
       				sum(hr.percentiles[16] * ce.percentage),
       				sum(hr.percentiles[17] * ce.percentage),
       				sum(hr.percentiles[18] * ce.percentage),
       				sum(hr.percentiles[19] * ce.percentage),
       				sum(hr.percentiles[20] * ce.percentage)
       			] as percentiles
  			FROM 
       			data.hif_result hr 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = hr.grid_cell_id					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
			GROUP BY
				ce.target_col, 
       			ce.target_row,
       			hr.hif_id,
       			hr.hif_instance_id
			ORDER BY
				ce.target_col, 
       			ce.target_row,
       			hr.hif_id,
       			hr.hif_instance_id;	
	else
		-- No crosswalk needed return the data straight from the table
		return query
			SELECT 
       			hr.grid_col, 
       			hr.grid_row,
       			hr.hif_id,
       			hr.hif_instance_id,
       			hr."result" as point_estimate,
       			hr.population as population,
       			hr.baseline_aq,
       			hr.scenario_aq,
       			hr.delta_aq,
       			hr.incidence,
       			hr.result_mean as mean,
				hr.baseline as baseline,
				hr.standard_dev as standard_dev,
       			hr.result_variance as "variance",
       			hr.pct_2_5 as pct_2_5,
       			hr.pct_97_5 as pct_97_5, 
       			hr.percentiles 
  			FROM 
       			data.hif_result hr 					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
			ORDER BY
       			hr.grid_col, 
       			hr.grid_row,
       			hr.hif_id,
       			hr.hif_instance_id;
	end if;	
end if;

end
$function$
;

--Valuation results
CREATE OR REPLACE FUNCTION data.get_valuation_results(_dataset_id integer, _hif_id integer[], _vf_id integer[], _output_grid_definition_id integer)
 RETURNS TABLE(grid_col integer, grid_row integer, hif_id integer, hif_instance_id integer, vf_id integer, point_estimate double precision, mean double precision, standard_dev double precision, variance double precision, pct_2_5 double precision, pct_97_5 double precision, percentiles double precision[])
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
	select vrd.grid_definition_id from data.valuation_result_dataset vrd where vrd.id = _dataset_id LIMIT 1
		into _source_grid_definition_id;
	if _source_grid_definition_id IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'Could not determine grid definition for result dataset';
	end if;
-- Aggregate to the whole study area
	if (_output_grid_definition_id = 0) then
		return query
			SELECT 
       			0 as target_col, 
       			0 as target_row,
       			vr.hif_id,
       			vr.hif_instance_id,
       			vr.vf_id,
       			sum(vr."result") as point_estimate,
       			sum(vr.result_mean) as mean,
				sum(vr.standard_dev) as standard_dev,
       			sum(vr.result_variance) as "variance",
       			sum(vr.pct_2_5) as pct_2_5,
       			sum(vr.pct_97_5) as pct_97_5,
       			ARRAY[
       				sum(vr.percentiles[1]),
       				sum(vr.percentiles[2]),
       				sum(vr.percentiles[3]),
       				sum(vr.percentiles[4]),
       				sum(vr.percentiles[5]),
       				sum(vr.percentiles[6]),
       				sum(vr.percentiles[7]),
       				sum(vr.percentiles[8]),
       				sum(vr.percentiles[9]),
       				sum(vr.percentiles[10]),
       				sum(vr.percentiles[11]),
       				sum(vr.percentiles[12]),
       				sum(vr.percentiles[13]),
       				sum(vr.percentiles[14]),
       				sum(vr.percentiles[15]),
       				sum(vr.percentiles[16]),
       				sum(vr.percentiles[17]),
       				sum(vr.percentiles[18]),
       				sum(vr.percentiles[19]),
       				sum(vr.percentiles[20])
       			] as percentiles
  			FROM 
       			data.valuation_result vr 		
 			WHERE
				vr.valuation_result_dataset_id = _dataset_id and
				(_hif_id IS NULL OR vr.hif_id = ANY(_hif_id)) and
				(_vf_id IS NULL OR vr.hif_id = ANY(_vf_id))
			GROUP BY
       			vr.hif_id,
       			vr.hif_instance_id,
       			vr.vf_id 
			ORDER BY
       			vr.hif_id,
       			vr.hif_instance_id,
       			vr.vf_id;
--Aggregate to the selected target grid	
	else
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
	       			vr.hif_instance_id,
	       			vr.vf_id,
	       			sum(vr."result" * ce.percentage) as point_estimate,
	       			sum(vr.result_mean * ce.percentage) as mean,
					sum(vr.standard_dev * ce.percentage) as standard_dev,
	       			sum(vr.result_variance * ce.percentage) as "variance",
	       			sum(vr.pct_2_5 * ce.percentage) as pct_2_5,
	       			sum(vr.pct_97_5 * ce.percentage) as pct_97_5,
	       			ARRAY[
	       				sum(vr.percentiles[1] * ce.percentage),
	       				sum(vr.percentiles[2] * ce.percentage),
	       				sum(vr.percentiles[3] * ce.percentage),
	       				sum(vr.percentiles[4] * ce.percentage),
	       				sum(vr.percentiles[5] * ce.percentage),
	       				sum(vr.percentiles[6] * ce.percentage),
	       				sum(vr.percentiles[7] * ce.percentage),
	       				sum(vr.percentiles[8] * ce.percentage),
	       				sum(vr.percentiles[9] * ce.percentage),
	       				sum(vr.percentiles[10] * ce.percentage),
	       				sum(vr.percentiles[11] * ce.percentage),
	       				sum(vr.percentiles[12] * ce.percentage),
	       				sum(vr.percentiles[13] * ce.percentage),
	       				sum(vr.percentiles[14] * ce.percentage),
	       				sum(vr.percentiles[15] * ce.percentage),
	       				sum(vr.percentiles[16] * ce.percentage),
	       				sum(vr.percentiles[17] * ce.percentage),
	       				sum(vr.percentiles[18] * ce.percentage),
	       				sum(vr.percentiles[19] * ce.percentage),
	       				sum(vr.percentiles[20] * ce.percentage)
	       			] as percentiles
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
	       			vr.hif_instance_id,
	       			vr.vf_id 
				ORDER BY
					ce.target_col, 
	       			ce.target_row,
	       			vr.hif_id,
	       			vr.hif_instance_id,
	       			vr.vf_id;	
		else
			-- No crosswalk needed return the data straight from the table
			return query
				SELECT 
	       			vr.grid_col, 
	       			vr.grid_row,
	       			vr.hif_id,
	       			vr.hif_instance_id,
	       			vr.vf_id,
	       			vr."result" as point_estimate,
	       			vr.result_mean as mean,
					vr.standard_dev as standard_dev,
	       			vr.result_variance as "variance",
	       			vr.pct_2_5 as pct_2_5,
	       			vr.pct_97_5 as pct_97_5,
	       			vr.percentiles
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
	       			vr.hif_instance_id,
	       			vr.vf_id;
		end if;	
	end if;

end
$function$
;
