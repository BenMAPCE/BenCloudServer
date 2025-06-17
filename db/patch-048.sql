/**** Assign AMI, SLD and Chen's Mortality functions to study groups ****/

UPDATE "data".settings SET value_int=48 WHERE "key"='version';

--remove assignments if already exist
DELETE FROM "data".health_impact_function_group_member WHERE health_impact_function_id in (1036,1037,1038,1039,1040,1041,1042,1043,1044);

--add new assignments
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (2,1036);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (2,1037);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (2,1038);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (2,1039);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (2,1040);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (6,1041);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (6,1042);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (6,1043);	
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (3,1041);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (3,1042);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (3,1043);	
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (1,1044);
INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
	VALUES (5,1044);