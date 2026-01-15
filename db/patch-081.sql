-- Associated with BWD 205 Create a new Health Impacts Function group called “Chronic Effects - Primary”. 
-- Add the following HIFs from the current “Chronic Effects - All”:
-- Ozone: Tetrault asthma, Parker hay fever/rhinitis
-- PM: Wei AMI, Tetrault asthma, Parker hay fever/rhinitis, Gharibvand lung cancer,Ensor OOH cardiac arrest,Rosenthal OOH cardiac arrest, Silverman OOH cardiac Arrest, Kloog stroke, Wilker et al., new dementia function
-- UPDATE "data".settings SET value_int=64 WHERE "key"='version'; --need to update version when this is approved


UPDATE "data".settings SET value_int=81 where "key"='version';


INSERT INTO data.health_impact_function_group("name", "help_text", "share_scope") 
      select   'Chronic Effects - Primary', 'Primary funtions selected from the larger chronic effects group', 1
where not exists (
  select 1 from data.health_impact_function_group where name = 'Chronic Effects - Primary'
);


CREATE TABLE "data".tmp_health_impact_function(
    "health_impact_function_id" int4 NULL
);

INSERT INTO "data".tmp_health_impact_function ("health_impact_function_id") VALUES
(1047),
(1048),
(966),
(888),
(889),
(968),
(969),
(871),
(887),
(976),
(1045),
(1046),
(1050),
(1051),
(965),
(1049),
(1018),
(964),
(963),
(962);


insert into data.health_impact_function_group_member("health_impact_function_group_id","health_impact_function_id")
select 
        hifg.id as "health_impact_function_group_id",
        tmp.health_impact_function_id  as "health_impact_function_id"
from 
    data.health_impact_function_group hifg 
 cross join
    data.tmp_health_impact_function tmp
 where
         hifg.name = 'Chronic Effects - Primary';

DROP TABLE "data".tmp_health_impact_function;

--Add new Dementia functions. Filter by author name as its id varies across platforms
 insert into data.health_impact_function_group_member("health_impact_function_group_id","health_impact_function_id")
 select 
         hifg.id as "health_impact_function_group_id",
         hif.id as "health_impact_function_id"
 from 
    data.health_impact_function_group hifg 
 cross join
    data.health_impact_function hif
 where
         hifg.name = 'Chronic Effects - Primary' 
 and 
         (
         (hif.pollutant_id = 6 AND 
          (author ILIKE '%wilker%')
         )) 
 and hif.health_impact_function_dataset_id = 15;
