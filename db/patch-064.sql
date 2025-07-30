/************************************************************************************************/
/**** Create table hif_result_agg to strore aggregated results for faster display in Results UI ****/
/************************************************************************************************/

UPDATE "data".settings SET value_int=64 WHERE "key"='version';

/****************Add table for aggregated results************/
--HIF
CREATE TABLE "data".hif_result_agg (
	hif_result_dataset_id int4 NULL,
	hif_id int4 NULL,
	grid_definition_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population float8 NULL,
	delta_aq float8 NULL,
	baseline_aq float8 NULL,
	scenario_aq float8 NULL,
	incidence float8 NULL,
	"result" float8 NULL,
	baseline float8 NULL,
	result_mean float8 NULL,
	standard_dev float8 NULL,
	result_variance float8 NULL,
	pct_2_5 float8 NULL,
	pct_97_5 float8 NULL,
	percentiles _float8 NULL,
	hif_instance_id int4 NULL
);
CREATE INDEX hif_result_hif_result_agg_idx ON data.hif_result_agg USING btree (hif_result_dataset_id, hif_id, grid_definition_id, grid_col, grid_row, grid_cell_id, result, population, baseline_aq, scenario_aq, delta_aq, incidence, result_mean, baseline, standard_dev, result_variance, pct_2_5, pct_97_5);
--VALULATION
CREATE TABLE "data".valuation_result_agg (
	valuation_result_dataset_id int4 NULL,
	vf_id int4 NULL,
	hif_id int4 NULL,
	grid_definition_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	population float8 NULL,
	"result" float8 NULL,
	result_mean float8 NULL,
	standard_dev float8 NULL,
	result_variance float8 NULL,
	pct_2_5 float8 NULL,
	pct_97_5 float8 NULL,
	percentiles _float8 NULL,
	hif_instance_id int4 NULL,
	vf_instance_id int4 NULL
);
CREATE INDEX valuation_result_agg_idx ON data.valuation_result_agg USING btree (valuation_result_dataset_id, vf_id, hif_id, grid_definition_id, grid_col, grid_row, grid_cell_id, population, result, result_mean, standard_dev, result_variance, pct_2_5, pct_97_5);
--EXPOSURE
CREATE TABLE "data".exposure_result_agg (
	exposure_result_dataset_id int4 NULL,
	exposure_function_id int4 NULL,
	grid_definition_id int4 NULL,
	grid_col int4 NULL,
	grid_row int4 NULL,
	grid_cell_id int8 NULL,
	subgroup_population float8 NULL,
	all_population float8 NULL,
	delta_aq float8 NULL,
	baseline_aq float8 NULL,
	scenario_aq float8 NULL,
	exposure_function_instance_id int4 NULL,
	"result" float8 NULL
);
CREATE INDEX exposure_result_agg_exposure_result_dataset_id_idx ON data.exposure_result_agg USING btree (exposure_result_dataset_id);

/****************Add functions for appending aggregated results ************/
--HIF
CREATE OR REPLACE FUNCTION data.add_hif_results_agg(_dataset_id integer, _output_grid_definition_id integer)
 RETURNS void
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

--delete old results
	delete from data.hif_result_agg where hif_result_dataset_id = _dataset_id and grid_definition_id = _output_grid_definition_id;


if (_output_grid_definition_id = 0) then
	
	--Aggregate the insert
	INSERT INTO "data".hif_result_agg
	(hif_result_dataset_id, hif_id, grid_definition_id,grid_col, grid_row, grid_cell_id, population, delta_aq, baseline_aq, scenario_aq
	, incidence, "result", baseline, hif_instance_id, percentiles, pct_2_5, pct_97_5)
	SELECT 
				hr.hif_result_dataset_id ,
				hr.hif_id,
				0 as grid_definition_id,
	   			0 as grid_col, 
	   			0 as grid_row,
	   			0 as grid_cell_id,
	   			sum(hr.population) as population,
	   			case when sum(hr.population) = 0 then 0 else sum(hr.delta_aq * hr.population) / sum(hr.population) end as delta_aq,
	   			case when sum(hr.population) = 0 then 0 else sum(hr.baseline_aq * hr.population) / sum(hr.population) end as baseline_aq, 
				case when sum(hr.population) = 0 then 0 else sum(hr.scenario_aq * hr.population) / sum(hr.population) end as scenario_aq,
	   			sum(hr.incidence) as incidence,
	   			sum(hr."result") as result,
	   			sum(hr.baseline) as baseline,
	   			hr.hif_instance_id,
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
	   			] as percentiles,
	   			sum(hr.percentiles[1]) as pct_2_5,
	   			sum(hr.percentiles[20]) as pct_97_5
	FROM 
	   			data.hif_result hr 					
			WHERE
				hr.hif_result_dataset_id = _dataset_id 
			GROUP BY
	   			hr.hif_result_dataset_id,
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
		INSERT INTO "data".hif_result_agg
		(hif_result_dataset_id, hif_id, grid_definition_id,grid_col, grid_row, grid_cell_id, population, delta_aq, baseline_aq, scenario_aq
		, incidence, "result", baseline, hif_instance_id, percentiles, pct_2_5, pct_97_5)
		SELECT 
       			hr.hif_result_dataset_id ,
				hr.hif_id,
				_output_grid_definition_id as grid_definition_id,
				ce.target_col, 
       			ce.target_row,
       			ce.target_grid_cell_id, 
       			sum(hr.population * ce.percentage) as population,
       			case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.delta_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as delta_aq,
       			case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.baseline_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as baseline_aq, 
				case when sum(hr.population * ce.percentage) = 0 then 0 else sum(hr.scenario_aq * ce.percentage * hr.population) / sum(hr.population * ce.percentage) end as scenario_aq,
				sum(hr.incidence * ce.percentage) as incidence,				
       			sum(hr."result" * ce.percentage) as result,
       			sum(hr.baseline * ce.percentage) as baseline,
				hr.hif_instance_id,
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
       			] as percentiles,
       			sum(hr.percentiles[1] * ce.percentage) as pct_2_5,
       			sum(hr.percentiles[20] * ce.percentage) as pct_97_5	
  			FROM 
       			data.hif_result hr 
			Inner Join
				data.crosswalk_entry ce On
	   				ce.crosswalk_id = _crosswalk_dataset_id AND
					ce.source_grid_cell_id = hr.grid_cell_id					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id 
			GROUP BY
				hr.hif_result_dataset_id,
				ce.target_grid_cell_id,
				ce.target_col, 
       			ce.target_row,
       			hr.hif_id,
       			hr.hif_instance_id;	
			
	else
		-- No need to append to agg table when source grid = target grid
		
	end if;	
end if;

--Recalculate result_mean,standard_dev,result_variance,pct_2_5,pct_97_5
update "data".hif_result_agg
set result_mean = (SELECT AVG(val) FROM unnest(percentiles) AS val)
,standard_dev = (select STDDEV_SAMP(val) FROM unnest(percentiles) AS val)
,result_variance = (select VAR_SAMP(val) FROM unnest(percentiles) AS val) 
where hif_result_dataset_id = _dataset_id and grid_definition_id = _output_grid_definition_id;

end
$function$
;

/****************Update get results functions ************/
--HIF
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

--if already exists in result_agg table, pull from there
if exists(SELECT 1 FROM data.hif_result_agg WHERE grid_definition_id = _output_grid_definition_id AND hif_result_dataset_id = _dataset_id) then
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
       			data.hif_result_agg hr 					
 			WHERE
				hr.hif_result_dataset_id = _dataset_id and
				hr.grid_definition_id = _output_grid_definition_id and
				(_hif_id IS NULL OR hr.hif_id = ANY(_hif_id))
			ORDER BY
       			hr.grid_col, 
       			hr.grid_row,
       			hr.hif_id,
       			hr.hif_instance_id;

			RETURN;
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




/****************VACUUM************/

VACUUM (VERBOSE, ANALYZE);