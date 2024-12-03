-- Adding support for generating complementary exposure results

UPDATE "data".settings SET value_int=32 WHERE "key"='version';


ALTER TABLE "data".exposure_function ADD function_type text NULL;
UPDATE "data".exposure_function SET function_type = 'Race' WHERE id in (2,3,4,5);
UPDATE "data".exposure_function SET function_type = 'Ethnicity' WHERE id = 7;
UPDATE "data".exposure_function SET function_type = 'Sex' WHERE id = 8; 
UPDATE "data".exposure_function SET function_type = 'Ages' WHERE id in (10,11,12);
UPDATE "data".exposure_function SET function_type = 'Poverty Status' WHERE id in (13,22);
UPDATE "data".exposure_function SET function_type = 'Educational Attainment' WHERE id = 15; 
UPDATE "data".exposure_function SET function_type = 'Employment Type' WHERE id = 17; 
UPDATE "data".exposure_function SET function_type = 'Insurance Status' WHERE id in (18,19,20,21);
UPDATE "data".exposure_function SET function_type = 'Linguistic Isolation' WHERE id in (24,25,26,27);


ALTER TABLE "data".exposure_result_function_config ADD function_type text null;
