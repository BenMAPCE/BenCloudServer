/************************************************************************************************/
/***Change population_growth table to partition table. Update get_population function.****/
/************************************************************************************************/
  
/********************************************************************************************************************************/
/***2020 population, crosswalks, incidence, and variable data are ready to import but too large to fit in the git repository.****/
/***Please contact the BenMAP development team for access to these files.********************************************************/
/********************************************************************************************************************************/

UPDATE "data".settings SET value_int=34 WHERE "key"='version';


/***Change population_growth table to partition table. ****/
--backup current 2010 data
ALTER TABLE "data".population_growth  RENAME TO population_growth_2010_backup;

--create partitioned table
ALTER INDEX IF EXISTS "data".zzz_population_growth_pk RENAME TO zzz_population_growth_backup_pk;

CREATE TABLE "data".population_growth (
	base_pop_year int2 NOT NULL,
	pop_year int2 NOT NULL,
	race_id int4 NOT NULL,
	gender_id int4 NOT NULL,
	ethnicity_id int4 NOT NULL,
	age_range_id int4 NOT NULL,
	grid_cell_id int4 NOT NULL,
	growth_value float8 NULL,
	CONSTRAINT population_growth_pk PRIMARY KEY (base_pop_year, pop_year, race_id, gender_id, ethnicity_id, age_range_id, grid_cell_id)
)
PARTITION BY LIST (base_pop_year);

CREATE INDEX population_growth_race_ethnicity_grid_idx ON data.population_growth USING btree (race_id, ethnicity_id, grid_cell_id);
CREATE INDEX population_growth_race_ethnicity_grid_age_gender_idx ON data.population_growth (race_id, ethnicity_id, grid_cell_id, age_range_id, gender_id);
CREATE INDEX population_growth_base_pop_year_idx ON "data".population_growth (base_pop_year);

ALTER TABLE "data".population_value ADD CONSTRAINT population_value_pk PRIMARY KEY (pop_entry_id,grid_cell_id);

--create partitions 
create table data.population_growth_b2010 partition of population_growth for values in (2010) partition by LIST (pop_year); 

--create subpaartitions
create table data.population_growth_b2010_t2000 partition of population_growth_b2010 for values in (2000); 
create table data.population_growth_b2010_t2001 partition of population_growth_b2010 for values in (2001); 
create table data.population_growth_b2010_t2002 partition of population_growth_b2010 for values in (2002); 
create table data.population_growth_b2010_t2003 partition of population_growth_b2010 for values in (2003); 
create table data.population_growth_b2010_t2004 partition of population_growth_b2010 for values in (2004); 
create table data.population_growth_b2010_t2005 partition of population_growth_b2010 for values in (2005); 
create table data.population_growth_b2010_t2006 partition of population_growth_b2010 for values in (2006); 
create table data.population_growth_b2010_t2007 partition of population_growth_b2010 for values in (2007); 
create table data.population_growth_b2010_t2008 partition of population_growth_b2010 for values in (2008); 
create table data.population_growth_b2010_t2009 partition of population_growth_b2010 for values in (2009); 
create table data.population_growth_b2010_t2010 partition of population_growth_b2010 for values in (2010); 
create table data.population_growth_b2010_t2011 partition of population_growth_b2010 for values in (2011); 
create table data.population_growth_b2010_t2012 partition of population_growth_b2010 for values in (2012); 
create table data.population_growth_b2010_t2013 partition of population_growth_b2010 for values in (2013); 
create table data.population_growth_b2010_t2014 partition of population_growth_b2010 for values in (2014); 
create table data.population_growth_b2010_t2015 partition of population_growth_b2010 for values in (2015); 
create table data.population_growth_b2010_t2016 partition of population_growth_b2010 for values in (2016); 
create table data.population_growth_b2010_t2017 partition of population_growth_b2010 for values in (2017); 
create table data.population_growth_b2010_t2018 partition of population_growth_b2010 for values in (2018); 
create table data.population_growth_b2010_t2019 partition of population_growth_b2010 for values in (2019); 
create table data.population_growth_b2010_t2020 partition of population_growth_b2010 for values in (2020); 
create table data.population_growth_b2010_t2021 partition of population_growth_b2010 for values in (2021); 
create table data.population_growth_b2010_t2022 partition of population_growth_b2010 for values in (2022); 
create table data.population_growth_b2010_t2023 partition of population_growth_b2010 for values in (2023); 
create table data.population_growth_b2010_t2024 partition of population_growth_b2010 for values in (2024); 
create table data.population_growth_b2010_t2025 partition of population_growth_b2010 for values in (2025); 
create table data.population_growth_b2010_t2026 partition of population_growth_b2010 for values in (2026); 
create table data.population_growth_b2010_t2027 partition of population_growth_b2010 for values in (2027); 
create table data.population_growth_b2010_t2028 partition of population_growth_b2010 for values in (2028); 
create table data.population_growth_b2010_t2029 partition of population_growth_b2010 for values in (2029); 
create table data.population_growth_b2010_t2030 partition of population_growth_b2010 for values in (2030); 
create table data.population_growth_b2010_t2031 partition of population_growth_b2010 for values in (2031); 
create table data.population_growth_b2010_t2032 partition of population_growth_b2010 for values in (2032); 
create table data.population_growth_b2010_t2033 partition of population_growth_b2010 for values in (2033); 
create table data.population_growth_b2010_t2034 partition of population_growth_b2010 for values in (2034); 
create table data.population_growth_b2010_t2035 partition of population_growth_b2010 for values in (2035); 
create table data.population_growth_b2010_t2036 partition of population_growth_b2010 for values in (2036); 
create table data.population_growth_b2010_t2037 partition of population_growth_b2010 for values in (2037); 
create table data.population_growth_b2010_t2038 partition of population_growth_b2010 for values in (2038); 
create table data.population_growth_b2010_t2039 partition of population_growth_b2010 for values in (2039); 
create table data.population_growth_b2010_t2040 partition of population_growth_b2010 for values in (2040); 
create table data.population_growth_b2010_t2041 partition of population_growth_b2010 for values in (2041); 
create table data.population_growth_b2010_t2042 partition of population_growth_b2010 for values in (2042); 
create table data.population_growth_b2010_t2043 partition of population_growth_b2010 for values in (2043); 
create table data.population_growth_b2010_t2044 partition of population_growth_b2010 for values in (2044); 
create table data.population_growth_b2010_t2045 partition of population_growth_b2010 for values in (2045); 
create table data.population_growth_b2010_t2046 partition of population_growth_b2010 for values in (2046); 
create table data.population_growth_b2010_t2047 partition of population_growth_b2010 for values in (2047); 
create table data.population_growth_b2010_t2048 partition of population_growth_b2010 for values in (2048); 
create table data.population_growth_b2010_t2049 partition of population_growth_b2010 for values in (2049); 
create table data.population_growth_b2010_t2050 partition of population_growth_b2010 for values in (2050); 
create table data.population_growth_b2010_t2055 partition of population_growth_b2010 for values in (2055); 

--insert growth data from backup table
INSERT INTO "data".population_growth (base_pop_year, pop_year, race_id, gender_id, ethnicity_id, age_range_id, grid_cell_id, growth_value)
select base_pop_year, pop_year, race_id, gender_id, ethnicity_id, age_range_id, grid_cell_id, growth_value from "data".population_growth_2010_backup;

--remove backup table. 
DROP TABLE "data".population_growth_2010_backup;

--create a material view to store base_pop_year for each data set. 
--when running get_population function, we use base_year to select population growth dataset. 
CREATE MATERIALIZED VIEW "data".mat_pop_dataset_base_year
TABLESPACE pg_default
AS with dataset_baseyear as (
  select distinct pe.pop_dataset_id,pe.pop_year from data.population_entry pe 
  )
  , growth_baseyear as (
  select distinct pg.base_pop_year from data.population_growth pg
  )
  select pop_dataset_id, gb.base_pop_year from dataset_baseyear db
  JOIN growth_baseyear gb ON db.pop_year = gb.base_pop_year
WITH DATA;


/*** Update get_population function to raise exception when projection is not available.****/
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
	SELECT base_pop_year
	FROM data.mat_pop_dataset_base_year mpdby 
	WHERE mpdby.pop_dataset_id = _dataset_id LIMIT 1
		into _base_year;
	if _base_year IS NULL then
		RAISE EXCEPTION 'Invalid Parameter - dataset_id = %', _dataset_id USING HINT = 'No population growth data available';
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



VACUUM (VERBOSE, ANALYZE);