-- Associated with BWD 205 which created a new Health Impacts Function group called “Chronic Effects - Primary” and added functions to it. 
-- This data patch is to ensure functions in “Chronic Effects - Primary” are also be in “Chronic Effects - All”

UPDATE "data".settings SET value_int=82 where "key"='version';

--1. target hif group is default group. 
--2. source hifs are default functions. 
--3. do not add function ids to the group member table if it's already there and assigned to the correct group. 
insert into data.health_impact_function_group_member("health_impact_function_group_id","health_impact_function_id")
select hifgLU.id as health_impact_function_group_id, hifgm.health_impact_function_id
from data.health_impact_function_group_member hifgm 
inner join data.health_impact_function_group hifg on hifgm.health_impact_function_group_id = hifg.id 
inner join data.health_impact_function_group hifgLU on hifgLU."name" = 'Chronic Effects - All'
inner join data.health_impact_function hif on hifgm.health_impact_function_id = hif.id 
where hifg."name" = 'Chronic Effects - Primary' 
and coalesce(hifg.user_id,'')=''
and coalesce(hif.user_id,'')=''
and hifgm.health_impact_function_id not in (
  select health_impact_function_id from data.health_impact_function_group_member hifgm2
  inner join data.health_impact_function_group hifg2 on hifgm2.health_impact_function_group_id = hifg2.id 
  where hifg2."name" = 'Chronic Effects - All' and coalesce(hifg2.user_id,'')=''
)
