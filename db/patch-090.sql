/*Add mortality, respiratory, infant health effect */
/*Change valuation_function.discounted values from true/false to yes/no */


DO $$
DECLARE
    v_old_endpoint_id bigint;
    v_new_endpoint_id bigint;
BEGIN

	UPDATE "data".settings SET value_int=90 where "key"='version';

    -- Find existing health effect
    SELECT id INTO v_old_endpoint_id
    FROM "data".endpoint
    WHERE name ILIKE 'Mortality, Respiratory, infant';

    -- Insert new health effect
    INSERT INTO "data".endpoint (endpoint_group_id, name, display_name)	
    VALUES (12, 'Mortality, Respiratory, infant', 'Mortality, Respiratory')
    RETURNING id INTO v_new_endpoint_id;

    -- If an existing health effect record existed, update references
    IF v_old_endpoint_id IS NOT NULL THEN
        UPDATE "data".health_impact_function SET endpoint_id = v_new_endpoint_id WHERE endpoint_id = v_old_endpoint_id;
        UPDATE "data".valuation_function SET endpoint_id = v_new_endpoint_id WHERE endpoint_id = v_old_endpoint_id;
        UPDATE "data".incidence_entry SET endpoint_id = v_new_endpoint_id WHERE endpoint_id = v_old_endpoint_id;

        DELETE FROM "data".endpoint WHERE id = v_old_endpoint_id;
    END IF;


/*Update valuation_function.discounted values*/
UPDATE "data".valuation_function set discounted = 'no' where discounted = 'false'; 
UPDATE "data".valuation_function set discounted = 'yes' where discounted = 'true';

END $$;
