-- 11/13/2024

UPDATE "data".settings SET value_int=29 where "key"='version';


-- Update population growth tables to allow having multiple growth datasets

-- Add base_pop_year column to pop growth table
ALTER TABLE "data".population_growth ADD COLUMN base_pop_year int2;
update "data".population_growth set base_pop_year = 2010;
ALTER TABLE "data".population_growth ALTER COLUMN base_pop_year SET NOT NULL;

-- update primary keys
alter table data.population_growth drop constraint population_growth_pk;
alter table data.population_growth add CONSTRAINT population_growth_pk PRIMARY KEY (race_id, gender_id, ethnicity_id, age_range_id, grid_cell_id, pop_year, base_pop_year);
drop index data."population_growth_wt_idx";
CREATE INDEX population_growth_idx ON data.population_growth USING btree (pop_year, race_id, ethnicity_id, grid_cell_id,base_pop_year);

-- Rename the pop_year column to base_pop_year in pop growth weight table
ALTER TABLE "data".population_growth_weight RENAME COLUMN pop_year TO base_pop_year;

-- Update get_population FUNCTION
-- DROP FUNCTION "data".get_population(int4, int4, _int4, _int4, _int4, _int4, bool, bool, bool, bool, int4);
CREATE OR REPLACE FUNCTION data.get_population(_dataset_id integer, _year integer, _race_id integer[], _ethnicity_id integer[], _gender_id integer[], _age_range_id integer[], _group_by_race boolean, _group_by_ethnicity boolean, _group_by_gender boolean, _group_by_age_range boolean, _output_grid_definition_id integer)
 RETURNS TABLE(grid_cell_id bigint, race_id integer, ethnicity_id integer, gender_id integer, age_range_id integer, pop_value double precision)
 LANGUAGE plpgsql
AS $function$
declare
	_source_grid_definition_id integer;
	_crosswalk_dataset_id integer;
	_known_pop_years integer[];
	_base_year integer;
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
-- Get known pop years
	SELECT array_agg(DISTINCT pe.pop_year) FROM data.population_entry pe WHERE pe.pop_dataset_id = _dataset_id
		into _known_pop_years;
-- Get base year for projection
	SELECT pe.pop_year 
	FROM data.population_entry pe 
	INNER JOIN data.population_growth pg on pe.pop_year = pg.base_pop_year 
	WHERE pe.pop_dataset_id = _dataset_id LIMIT 1
		into _base_year;

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
		if 	_year = ANY(_known_pop_years) then
			--Crosswalk needed, No projection needed
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
			-- Crosswalk needed, Projection needed
			return query					
				select
					c.target_grid_cell_id as grid_cell_id,
					case when _group_by_race then pe.race_id else null end, 
					case when _group_by_ethnicity then pe.ethnicity_id else null end,
					case when _group_by_gender then pe.gender_id else null end,
					case when _group_by_age_range then pe.age_range_id else null end,
					sum(pv.pop_value * g.growth_rate * c.percentage)
				from data.population_entry pe
				join data.population_value pv on pe.id = pv.pop_entry_id and pe.pop_dataset_id = _dataset_id
				join (select
					pgw.target_grid_cell_id as grid_cell_id
					, pg.race_id 
					, pg.ethnicity_id 
					, pg.gender_id 
					, pg.age_range_id 
					, pg.pop_year as target_pop_year
					, pgw.base_pop_year
					, sum(pg.growth_value * pgw.growth_weight) as growth_rate
					from data.population_growth pg
					join data.population_growth_weight pgw 
					 on pg.grid_cell_id = pgw.source_grid_cell_id 
					 and pg.ethnicity_id = pgw.ethnicity_id 
					 and pg.race_id = pgw.race_id 
					 and pg.base_pop_year = pgw.base_pop_year
					where pg.pop_year = _year AND
					pg.base_pop_year = _base_year AND
					pgw.pop_dataset_id = _dataset_id AND
					(_race_id IS NULL OR pg.race_id = ANY (_race_id)) AND
					(_ethnicity_id IS NULL OR pg.ethnicity_id = ANY (_ethnicity_id)) AND
					(_gender_id IS NULL OR pg.gender_id = ANY (_gender_id)) AND
					(_age_range_id IS NULL OR pg.age_range_id = ANY (_age_range_id))
					group by 1,2,3,4,5,6,7) g
					on pv.grid_cell_id = g.grid_cell_id 
					and pe.race_id = g.race_id 
					and pe.gender_id = g.gender_id 
					and pe.ethnicity_id = g.ethnicity_id 
					and pe.age_range_id = g.age_range_id
					and pe.pop_year  = g.base_pop_year
				JOIN data.crosswalk_entry c On
		   				c.crosswalk_id = _crosswalk_dataset_id AND
						c.source_grid_cell_id = g.grid_cell_id
				GROUP BY
					c.target_grid_cell_id,
					case when _group_by_race then pe.race_id else null end, 
					case when _group_by_ethnicity then pe.ethnicity_id else null end,
					case when _group_by_gender then pe.gender_id else null end,
					case when _group_by_age_range then pe.age_range_id else null end
				ORDER BY
					c.target_grid_cell_id;
		end if;	
	else
		-- No crosswalk needed
		if 	_year = ANY(_known_pop_years) then
			-- No crosswalk needed, No projection needed		
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
		else			
			-- No crosswalk needed, projection needed
			return query		
					select
						g.grid_cell_id,
						case when _group_by_race then pe.race_id else null end, 
						case when _group_by_ethnicity then pe.ethnicity_id else null end,
						case when _group_by_gender then pe.gender_id else null end,
						case when _group_by_age_range then pe.age_range_id else null end,
						sum(pv.pop_value * g.growth_rate)
					from data.population_entry pe
					join data.population_value pv on pe.id = pv.pop_entry_id and pe.pop_dataset_id = _dataset_id
					join (select
						pgw.target_grid_cell_id as grid_cell_id
						, pg.race_id 
						, pg.ethnicity_id 
						, pg.gender_id 
						, pg.age_range_id 
						, pg.pop_year as target_pop_year
						, pgw.base_pop_year
						, sum(pg.growth_value * pgw.growth_weight) as growth_rate
						from data.population_growth pg
						join data.population_growth_weight pgw 
						 on pg.grid_cell_id = pgw.source_grid_cell_id 
						 and pg.ethnicity_id = pgw.ethnicity_id 
						 and pg.race_id = pgw.race_id
						 and pg.base_pop_year = pgw.base_pop_year
					where pg.pop_year = _year AND
					pg.base_pop_year = _base_year AND
						pgw.pop_dataset_id = _dataset_id AND
						(_race_id IS NULL OR pg.race_id = ANY (_race_id)) AND
						(_ethnicity_id IS NULL OR pg.ethnicity_id = ANY (_ethnicity_id)) AND
						(_gender_id IS NULL OR pg.gender_id = ANY (_gender_id)) AND
						(_age_range_id IS NULL OR pg.age_range_id = ANY (_age_range_id))
						group by 1,2,3,4,5,6,7) g
						on pv.grid_cell_id = g.grid_cell_id 
						and pe.race_id = g.race_id 
						and pe.gender_id = g.gender_id 
						and pe.ethnicity_id = g.ethnicity_id 
						and pe.age_range_id = g.age_range_id
						and pe.pop_year  = g.base_pop_year 
					GROUP BY
						g.grid_cell_id,
						case when _group_by_race then pe.race_id else null end, 
						case when _group_by_ethnicity then pe.ethnicity_id else null end,
						case when _group_by_gender then pe.gender_id else null end,
						case when _group_by_age_range then pe.age_range_id else null end
					ORDER BY
						g.grid_cell_id;
		end if;
	end if;	
end
$function$
;
vacuum analyze;
