/*******************************************
Associated with BWD-206 -- Update Mortality Health Effects and Health Impact Functions
Step 1: Create four new Health Effects within the Mortality Health Effect Category: 
“Mortality, All-cause, short-term”
“Mortality, Respiratory, short-term”
“Mortality, All-cause, long-term”
“Mortality, Respiratory, long-term”
Assign mortality functions whose Timing field is Daily to the short-term categories. Assign mortality HIFs whose Timing is Annual to the long-term categories.
Change the linkage between the mortality HIFs and EPA’s recommended valuation functions so that:
    1.the Long-term HIFs remain linked to the same valuation functions used currently. (i.e., no change for these)
    2.the short-term HIFs are linked to only one EPA recommended valuation function, the function that uses the undiscounted VSL.

The old “Mortality, All-cause” and “Mortality, Respiratory” Health Effects should be archived/no longer shown as options when a list of health effects is shown to the user.
As a test, once completed, the following functions should appear under the short-term categories and should be associated with the undiscounted VSL on the Valuation screen.

Ozone:
Katsouyanni et al, 2009
Medina-Ramon and Schwartz, 2008
Zanobetti and Schwartz, 2008
*******************************************/

UPDATE data.settings SET value_int=77 WHERE "key"='version';


-- Step 1: Create new Health Effects and store their IDs
--  Create four new Health Effects within the Mortality Health Effect Category: 
--“Mortality, All-cause, short-term”
-- “Mortality, Respiratory, short-term”
--“Mortality, All-cause, long-term”
--“Mortality, Respiratory, long-term”
WITH new_values AS (
    SELECT * FROM (VALUES
        (12, 'Mortality, All-cause, short-term'),
        (12, 'Mortality, Respiratory, short-term'),
        (12, 'Mortality, All-cause, long-term'),
        (12, 'Mortality, Respiratory, long-term')
    ) AS v(endpoint_group_id, name)
    WHERE NOT EXISTS (
        SELECT 1 FROM data.endpoint e WHERE e.name = v.name
    )
),
new_endpoint AS (
    INSERT INTO data.endpoint (endpoint_group_id, name)
    SELECT endpoint_group_id, name FROM new_values
    RETURNING id, name, endpoint_group_id
)
SELECT * INTO data.temp_endpoint FROM new_endpoint;
;


-- Step 2: Create Temporary Table for Health Impact Functions
-- Copy relevant health impact functions to a temporary table for modification with new endpoint ids
create table "data".tmp_health_function as 
SELECT DISTINCT
    e.name, hif.id, hif.health_impact_function_dataset_id, hif.endpoint_group_id, hif.endpoint_id, hif.pollutant_id, hif.metric_id, hif.seasonal_metric_id,
	hif.metric_statistic, hif.author, hif.function_year, hif.location, hif.other_pollutants, hif.qualifier, hif.reference, hif.start_age, hif.end_age, hif.function_text,
	hif.beta, hif.dist_beta, hif.p1_beta, hif.p2_beta, hif.val_a, hif.name_a,hif.val_b, hif.name_b, hif.val_c, hif.name_c, hif.baseline_function_text,
	hif.race_id, hif.gender_id, hif.ethnicity_id, hif.start_day, hif.end_day, hif.hero_id, hif.epa_hero_url, hif.access_url, hif.user_id, hif.share_scope,
	hif.archived, hif.timing_id, hif.geographic_area, hif.geographic_area_feature
FROM (
    SELECT 
        id AS endpoint_id, 
        name, 
        endpoint_group_id
    FROM "data".endpoint
) e
LEFT JOIN data.health_impact_function hif 
    ON e.endpoint_id = hif.endpoint_id 
    AND e.endpoint_group_id = hif.endpoint_group_id
WHERE e.name IN ('Mortality, All Cause', 'Mortality, Respiratory')
    AND hif.health_impact_function_dataset_id = 15;

-- Insert new health impact functions with updated endpoint ids from temporary table
-- Create new table with the new health impact functions ids, endpoint ids, endpoint group ids, and timing ids to use in next step
--Assign mortality functions whose Timing field is Daily to the short-term categories. Assign mortality HIFs whose Timing is Annual to the long-term categories.

WITH new_health_functions AS (
	INSERT INTO data.health_impact_function   (
    	health_impact_function_dataset_id, endpoint_group_id, endpoint_id, pollutant_id, metric_id,seasonal_metric_id,metric_statistic,author,function_year,location,other_pollutants,qualifier,reference,
    	start_age,end_age,function_text,beta,dist_beta,p1_beta,p2_beta,val_a,name_a,val_b,name_b,val_c,name_c,baseline_function_text,race_id,gender_id,ethnicity_id,start_day,end_day,hero_id,epa_hero_url,
    	access_url,user_id,share_scope,archived,timing_id,geographic_area,geographic_area_feature
	)
SELECT
    thf.health_impact_function_dataset_id,te.endpoint_group_id,te.id as endpoint_id, thf.pollutant_id, thf.metric_id, thf.seasonal_metric_id,thf.metric_statistic,thf.author, thf.function_year,
	thf.location, thf.other_pollutants, thf.qualifier, thf.reference, thf.start_age, thf.end_age, thf.function_text, thf.beta, thf.dist_beta, thf.p1_beta, thf.p2_beta, thf.val_a, thf.name_a,
	thf.val_b, thf.name_b, thf.val_c, thf.name_c, thf.baseline_function_text, thf.race_id, thf.gender_id, thf.ethnicity_id, thf.start_day, thf.end_day, thf.hero_id, thf.epa_hero_url, thf.access_url, thf.user_id,
	thf.share_scope, thf.archived, thf.timing_id, thf.geographic_area, thf.geographic_area_feature
FROM data.tmp_health_function thf 
LEFT JOIN  data.temp_endpoint te 
    ON te.name = CASE
        WHEN thf.name = 'Mortality, All Cause' AND thf.timing_id  = 2 THEN 'Mortality, All-cause, short-term'
        WHEN thf.name = 'Mortality, All Cause' AND thf.timing_id = 1 THEN 'Mortality, All-cause, long-term'
        WHEN thf.name = 'Mortality, Respiratory' AND thf.timing_id = 2 THEN 'Mortality, Respiratory, short-term'
        WHEN thf.name = 'Mortality, Respiratory' AND thf.timing_id = 1 THEN 'Mortality, Respiratory, long-term'
    END
WHERE thf.name IN ('Mortality, All Cause', 'Mortality, Respiratory')
RETURNING id, endpoint_id, endpoint_group_id,timing_id
)
SELECT * INTO TABLE "data".new_hif_functions FROM new_health_functions;

DROP TABLE "data".new_hif_functions;
DROP TABLE "data".tmp_health_function;
--Archive Health Impact functions the old “Mortality, All-cause” and “Mortality, Respiratory” Health Effects so they no longer appear as options to users
DELETE FROM data.health_impact_function_group_member
WHERE health_impact_function_id IN (
    SELECT id
    FROM data.health_impact_function
    WHERE endpoint_id IN (50, 56)
      AND endpoint_group_id = 12
);

update data.health_impact_function hif
set archived = 1
where hif.endpoint_id IN (50,56) and hif.endpoint_group_id = 12 and hif.health_impact_function_dataset_id = 15; 



-- Step 3: Update Valuation Function Linkages
-- --Change the linkage between the mortality HIFs and EPA’s recommended valuation functions so that:
        --the Long-term HIFs remain linked to the same valuation functions used currently. (i.e., no change for these)
        --the short-term HIFs are linked to only one EPA recommended valuation function, the function that uses the undiscounted VSL. 

create table "data".tmp_val_all_cause_function as 
select 
	te.name, vf.valuation_dataset_id, te.endpoint_group_id, te.id as endpoint_id, vf.qualifier, vf.reference, vf.start_age, vf.end_age, vf.function_text, vf.val_a, vf.name_a, vf.dist_a,
	vf.p1a, vf.p2a, vf.val_b, vf.name_b, vf.val_c, vf.name_c, vf.val_d, vf.name_d, vf.epa_standard, vf.access_url, vf.valuation_type, vf.multiyear, vf.multiyear_dr, vf.multiyear_costs,
	vf.user_id, vf.share_scope, vf.archived
	from data.valuation_function vf 
	cross join data.temp_endpoint te 
	where vf.endpoint_group_id = 12 
		and vf.endpoint_id = 50
		and te.name ilike '%Mortality, All-cause%'
		and NOT (te.name ILIKE '%short_term%' AND vf.name_b ILIKE '%d.r%'); 

create table "data".tmp_val_resp_function as 
select 
	te.name, vf.valuation_dataset_id, te.endpoint_group_id, te.id as endpoint_id, vf.qualifier, vf.reference, vf.start_age, vf.end_age, vf.function_text, vf.val_a, vf.name_a, vf.dist_a,
	vf.p1a, vf.p2a, vf.val_b, vf.name_b, vf.val_c, vf.name_c, vf.val_d, vf.name_d, vf.epa_standard, vf.access_url, vf.valuation_type, vf.multiyear, vf.multiyear_dr, vf.multiyear_costs,
	vf.user_id, vf.share_scope, vf.archived
	from data.valuation_function vf 
	cross join data.temp_endpoint te 
	where vf.endpoint_group_id = 12 
		and vf.endpoint_id = 56
		and te.name ilike '%Mortality, Respiratory%'
		and NOT (te.name ILIKE '%short_term%' AND vf.name_b ILIKE '%d.r%'); 

update  data.tmp_val_all_cause_function
set epa_standard = true
where name ILIKE '%short_term%';

update  data.tmp_val_resp_function
set epa_standard = true
where name ILIKE '%short_term%';


update data.valuation_function vf
set epa_standard = false, 
    archived = 1
where vf.endpoint_group_id = 12
      and vf.valuation_dataset_id = 8;

INSERT INTO data.valuation_function   (
    	valuation_dataset_id, endpoint_group_id, endpoint_id ,qualifier ,reference ,start_age ,end_age ,function_text ,val_a ,name_a , dist_a ,p1a ,p2a, val_b, name_b, val_c,name_c
        ,val_d,name_d,epa_standard,access_url,valuation_type,multiyear,multiyear_dr,multiyear_costs,user_id,share_scope,archived
	)
select valuation_dataset_id, endpoint_group_id, endpoint_id ,qualifier ,reference ,start_age ,end_age ,function_text ,val_a ,name_a , dist_a ,p1a ,p2a, val_b, name_b, val_c,name_c
        ,val_d,name_d,epa_standard,access_url,valuation_type,multiyear,multiyear_dr,multiyear_costs,user_id,share_scope,archived  
from data.tmp_val_all_cause_function;

INSERT INTO data.valuation_function   (
    	valuation_dataset_id, endpoint_group_id, endpoint_id ,qualifier ,reference ,start_age ,end_age ,function_text ,val_a ,name_a , dist_a ,p1a ,p2a, val_b, name_b, val_c,name_c
        ,val_d,name_d,epa_standard,access_url,valuation_type,multiyear,multiyear_dr,multiyear_costs,user_id,share_scope,archived
	)
select valuation_dataset_id, endpoint_group_id, endpoint_id ,qualifier ,reference ,start_age ,end_age ,function_text ,val_a ,name_a , dist_a ,p1a ,p2a, val_b, name_b, val_c,name_c
        ,val_d,name_d,epa_standard,access_url,valuation_type,multiyear,multiyear_dr,multiyear_costs,user_id,share_scope,archived  
from data.tmp_val_resp_function;

DROP TABLE "data".tmp_val_all_cause_function;
DROP TABLE "data".tmp_val_resp_function;
DROP TABLE "data".temp_endpoint;
-- *******************************************