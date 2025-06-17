/**** update health impact function GROUPS.****/

UPDATE "data".settings SET value_int=44 WHERE "key"='version';
--
delete from "data".health_impact_function_group_member
where health_impact_function_id not in (select id from data.health_impact_function);

delete from data.health_impact_function_group_member hifgm 
where hifgm.health_impact_function_id in (1024,1032,1033,1027,1026,1025,1028,1034,1035,1031,1030,1029);

INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(3,1024);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(6,1024);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1032);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(5,1032);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1033);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1027);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1026);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1025);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(3,1028);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(6,1028);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1034);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(5,1034);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1035);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1031);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1030);
INSERT INTO "data".health_impact_function_group_member(health_impact_function_group_id, health_impact_function_id)VALUES(1,1029);

