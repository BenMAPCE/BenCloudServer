/*
    BWD-223 - update new dementia health effect display_name
    BWD-232 - remove Di et al., and Turner et al. PM functions from the “Premature Death – Primary” group
*/

UPDATE data.settings SET value_int=80 WHERE "key"='version';

UPDATE data.endpoint SET display_name = name WHERE name = 'Incidence, Dementia';

DELETE FROM data.health_impact_function_group_member 
    WHERE health_impact_function_group_id = 5 AND health_impact_function_id IN (
        SELECT id FROM data.health_impact_function 
            WHERE health_impact_function_dataset_id = 15 AND pollutant_id = 6 AND (author LIKE 'Di%' OR author LIKE 'Turner%')
    );