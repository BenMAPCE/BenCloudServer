/*Add mortality, respiratory, infant health effect */
/*Change valuation_function.discounted values from true/false to yes/no */


DO $$
DECLARE
    v_new_endpoint_id bigint;
BEGIN

	UPDATE "data".settings SET value_int=90 where "key"='version';

	CREATE TEMPORARY TABLE tmp_old_endpoint (
	    endpoint_id int4
	);	

    -- Find existing health effect in default Mortality group
    INSERT INTO tmp_old_endpoint(endpoint_id)
    SELECT id
    FROM "data".endpoint
    WHERE name ILIKE 'Mortality, Respiratory, infant' and endpoint_group_id = 12 and name <> 'Mortality, Respiratory, infant';

    -- Insert new health effect if not already exists
    INSERT INTO "data".endpoint (endpoint_group_id, name, display_name)	
    SELECT 12, 'Mortality, Respiratory, infant', 'Mortality, Respiratory'
	WHERE NOT EXISTS (SELECT 1 FROM "data".endpoint WHERE endpoint_group_id = 12 AND name = 'Mortality, Respiratory, infant')
    RETURNING id INTO v_new_endpoint_id;
	
	--Get v_new_endpoint_id if already exists
	IF v_new_endpoint_id IS NULL THEN
		SELECT id INTO v_new_endpoint_id FROM "data".endpoint WHERE endpoint_group_id = 12 AND name = 'Mortality, Respiratory, infant';
	END IF;

    -- If an existing health effect record existed, update references
    IF EXISTS (SELECT 1 FROM tmp_old_endpoint) THEN
        UPDATE "data".health_impact_function SET endpoint_id = v_new_endpoint_id WHERE endpoint_id IN (SELECT endpoint_id FROM tmp_old_endpoint) ;
        UPDATE "data".valuation_function SET endpoint_id = v_new_endpoint_id WHERE endpoint_id IN (SELECT endpoint_id FROM tmp_old_endpoint);
        UPDATE "data".incidence_entry SET endpoint_id = v_new_endpoint_id WHERE endpoint_id IN (SELECT endpoint_id FROM tmp_old_endpoint);

        DELETE FROM "data".endpoint WHERE id IN (SELECT endpoint_id FROM tmp_old_endpoint);
    END IF;


/*Update valuation_function.discounted values*/
UPDATE "data".valuation_function set discounted = 'no' where discounted = 'false'; 
UPDATE "data".valuation_function set discounted = 'yes' where discounted = 'true';

END $$;
