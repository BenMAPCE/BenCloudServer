/************************************************************************************************/
/**** Reassign ozone Katsouyanni mortality HIFs to different groups, and correct some HERO IDs ****/
/************************************************************************************************/

UPDATE "data".settings SET value_int=65 WHERE "key"='version';


/**** change ozone Katsouyanni et al. functions group assignments ****/
DELETE FROM "data".health_impact_function_group_member
	WHERE health_impact_function_group_id=5 AND health_impact_function_id=1032;
DELETE FROM "data".health_impact_function_group_member
	WHERE health_impact_function_group_id=5 AND health_impact_function_id=1034;

DELETE FROM "data".health_impact_function_group_member
	WHERE health_impact_function_group_id=5 AND health_impact_function_id=1025;
DELETE FROM "data".health_impact_function_group_member
	WHERE health_impact_function_group_id=5 AND health_impact_function_id=1029;
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (5,1025);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (5,1029);

/****************Correct HERO IDs and URL ************/	
UPDATE "data".health_impact_function SET hero_id=3420742, access_url='https://ehp.niehs.nih.gov/doi/10.1289/EHP124'
	WHERE id in (965,1045,1046,1047,1048,1049,1050,1051);
	
UPDATE "data".health_impact_function SET hero_id=195755 
	WHERE id in (922, 923, 924, 925, 926, 927, 928);

