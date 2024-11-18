-- Adding support for generating complementary exposure results

UPDATE "data".settings SET value_int=31 WHERE "key"='version';

ALTER TABLE "data".exposure_function ADD generate_complement boolean default false;
UPDATE "data".exposure_function SET generate_complement = case when id in (2,3,4,5,7,8,13,15,17,18,19,20,21,22) then true else false end;

ALTER TABLE "data".exposure_function ADD complement_name text NULL;
UPDATE "data".exposure_function SET complement_name = 'Non-Asian (0-99)' WHERE id = 2;
UPDATE "data".exposure_function SET complement_name = 'Non-Black (0-99)' WHERE id = 3; 
UPDATE "data".exposure_function SET complement_name = 'Non-Native American (0-99)' WHERE id = 4; 
UPDATE "data".exposure_function SET complement_name = 'Non-White (0-99)' WHERE id = 5; 
UPDATE "data".exposure_function SET complement_name = 'Non-Hispanic (0-99)' WHERE id = 7;
UPDATE "data".exposure_function SET complement_name = 'Male (0-99)' WHERE id = 8; 
UPDATE "data".exposure_function SET complement_name = 'Above Poverty Line (0-99)' WHERE id = 13; 
UPDATE "data".exposure_function SET complement_name = 'More educated (>24; high school or more)' WHERE id = 15; 
UPDATE "data".exposure_function SET complement_name = 'Non-Blue Collar Workers (0-99)' WHERE id = 17; 
UPDATE "data".exposure_function SET complement_name = 'Has Health Insurance (1-17)' WHERE id = 18; 
UPDATE "data".exposure_function SET complement_name = 'Has Health Insurance (18-39)' WHERE id = 19; 
UPDATE "data".exposure_function SET complement_name = 'Has Health Insurance (40-64)' WHERE id = 20; 
UPDATE "data".exposure_function SET complement_name = 'Has Health Insurance (<65)' WHERE id = 21; 
UPDATE "data".exposure_function SET complement_name = 'Above 2x Poverty Line (0-99)' WHERE id = 22; 


delete from "data".exposure_function WHERE id in (6,9,14,16,23);
delete from "data".exposure_function_group_member WHERE exposure_function_id in (6,9,14,16,23);

ALTER TABLE "data".exposure_result_function_config ADD population_group text null;
UPDATE "data".exposure_result_function_config SET population_group = 'All: Reference (0-99)' WHERE exposure_function_id = 1;
UPDATE "data".exposure_result_function_config SET population_group = 'Asian (0-99)' WHERE exposure_function_id = 2;
UPDATE "data".exposure_result_function_config SET population_group = 'Black (0-99)' WHERE exposure_function_id = 3;
UPDATE "data".exposure_result_function_config SET population_group = 'Native American (0-99)' WHERE exposure_function_id = 4;
UPDATE "data".exposure_result_function_config SET population_group = 'White (0-99)' WHERE exposure_function_id = 5;
UPDATE "data".exposure_result_function_config SET population_group = 'Non-Hispanic (0-99)' WHERE exposure_function_id = 6;
UPDATE "data".exposure_result_function_config SET population_group = 'Hispanic (0-99)' WHERE exposure_function_id = 7;
UPDATE "data".exposure_result_function_config SET population_group = 'Female (0-99)' WHERE exposure_function_id = 8;
UPDATE "data".exposure_result_function_config SET population_group = 'Male (0-99)' WHERE exposure_function_id = 9;
UPDATE "data".exposure_result_function_config SET population_group = 'Children (0-17)' WHERE exposure_function_id = 10;
UPDATE "data".exposure_result_function_config SET population_group = 'Adults (18-64)' WHERE exposure_function_id = 11;
UPDATE "data".exposure_result_function_config SET population_group = 'Older Adults (65-99)' WHERE exposure_function_id = 12;
UPDATE "data".exposure_result_function_config SET population_group = 'Below Poverty Line (0-99)' WHERE exposure_function_id = 13;
UPDATE "data".exposure_result_function_config SET population_group = 'Above Poverty Line (0-99)' WHERE exposure_function_id = 14;
UPDATE "data".exposure_result_function_config SET population_group = 'Less educated (>24; no high school)' WHERE exposure_function_id = 15;
UPDATE "data".exposure_result_function_config SET population_group = 'More educated (>24; high school or more)' WHERE exposure_function_id = 16;
UPDATE "data".exposure_result_function_config SET population_group = 'Blue Collar Workers (0-99)' WHERE exposure_function_id = 17;
UPDATE "data".exposure_result_function_config SET population_group = 'No Health Insurance (1-17)' WHERE exposure_function_id = 18;
UPDATE "data".exposure_result_function_config SET population_group = 'No Health Insurance (18-39)' WHERE exposure_function_id = 19;
UPDATE "data".exposure_result_function_config SET population_group = 'No Health Insurance (40-64)' WHERE exposure_function_id = 20;
UPDATE "data".exposure_result_function_config SET population_group = 'No Health Insurance (<65)' WHERE exposure_function_id = 21;
UPDATE "data".exposure_result_function_config SET population_group = 'Below 2x Poverty Line (0-99)' WHERE exposure_function_id = 22;
UPDATE "data".exposure_result_function_config SET population_group = 'Above 2x Poverty Line (0-99)' WHERE exposure_function_id = 23;
UPDATE "data".exposure_result_function_config SET population_group = 'Speaks English Less Than Very Well (0-99)' WHERE exposure_function_id = 24;
UPDATE "data".exposure_result_function_config SET population_group = 'Speaks English Less Than Well (0-99)' WHERE exposure_function_id = 25;
UPDATE "data".exposure_result_function_config SET population_group = 'Speaks English Very Well or Better (0-99)' WHERE exposure_function_id = 26;
UPDATE "data".exposure_result_function_config SET population_group = 'Speaks English Well or Better (0-99)' WHERE exposure_function_id = 27;

ALTER TABLE "data".exposure_result_function_config ADD hidden_sort_order text null;
UPDATE "data".exposure_result_function_config SET hidden_sort_order = population_group;
UPDATE "data".exposure_result_function_config SET hidden_sort_order = '00. All: Reference (0-99)' where exposure_function_id = 1;