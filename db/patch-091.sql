/*****The scripts are to tackle a potential issue when a default endpoint group (or health impact function group) have the same name as user-uploaded group. */
/*****It's an updated version of patch 084. 084 were treating dataset names in a case-sensitive way. This patch is case-insensitive*/
UPDATE "data".settings SET value_int=91 where "key"='version';

/*It's possible that when we added default datasets from the backend, they were linked to user-uploaded groups if the user-uploaded ones are already in the lookup tables.
  In this case, we add a new item into the lookup table with user = null. The functions or data will be rewired to this one in later steps.  */
--endpoint/health impact function
insert into data.endpoint_group (name, share_scope)
select distinct eg."name", 1
from data.health_impact_function hif 
inner join data.endpoint_group eg on hif.endpoint_group_id = eg.id 
where hif.user_id is null 
and eg.user_id  is not null
and not exists (select 1 from data.endpoint_group lu where lower(lu.name) = lower(eg."name") and lu.user_id is null);

--incidence_entry
insert into data.endpoint_group (name, share_scope)
select distinct eg."name", 1
from data.incidence_entry ie 
inner join data.incidence_dataset id on ie.incidence_dataset_id = id.id 
inner join data.endpoint_group eg on ie.endpoint_group_id = eg.id 
where id.user_id is null
and eg.user_id is not null
and not exists (select 1 from data.endpoint_group lu where lower(lu.name) = lower(eg."name") and lu.user_id is null);

--valuation_function
insert into data.endpoint_group (name, share_scope)
select eg."name", 1
from data.valuation_function vf 
inner join data.endpoint_group eg on vf.endpoint_group_id = eg.id 
where vf.user_id is null 
and eg.user_id  is not null
and not exists (select 1 from data.endpoint_group lu where lower(lu.name) = lower(eg."name") and lu.user_id is null);

--health_impact_function_group
update data.health_impact_function_group 
set user_id = null, share_scope = 1
from data.health_impact_function hif 
inner join data.health_impact_function_group_member hifgm on hif.id = hifgm.health_impact_function_id 
where hif.user_id is null 
and data.health_impact_function_group.user_id is not null
and data.health_impact_function_group.id = hifgm.health_impact_function_group_id; 

/*It's possible we have cases where a user-uploaded endpoint_group or hif_group have the same name as a default one in the same lookup table.
  In this case, we rewire the user-uploaded endpoint to the default one, and remove the user-uploaded one from the lookup table.  */

CREATE TEMPORARY TABLE tmp_conflict_id (
    user_upload_id INT,
    default_id INT
);
--select * from tmp_conflict_id

-----endpoint groups ----

--get endpoint group ids that have conflicts and store in a temp table
insert into tmp_conflict_id(user_upload_id, default_id)
select eg2.id as user_upload_id, eg1.id as default_id from data.endpoint_group eg1
inner join data.endpoint_group eg2 on lower(eg1.name)=lower(eg2."name") 
where coalesce(eg1.user_id,'')='' and coalesce(eg2.user_id,'')<>'';

--update affected tables to use default id. 
update data.endpoint 
set endpoint_group_id = tmp_conflict_id.default_id
from tmp_conflict_id
where data.endpoint.endpoint_group_id = tmp_conflict_id.user_upload_id;

update data.health_impact_function 
set endpoint_group_id = tmp_conflict_id.default_id
from tmp_conflict_id
where data.health_impact_function.endpoint_group_id = tmp_conflict_id.user_upload_id;

update data.incidence_entry 
set endpoint_group_id = tmp_conflict_id.default_id
from tmp_conflict_id
where data.incidence_entry.endpoint_group_id = tmp_conflict_id.user_upload_id;

update data.income_growth_adj_factor 
set endpoint_group_id = tmp_conflict_id.default_id
from tmp_conflict_id
where data.income_growth_adj_factor.endpoint_group_id = tmp_conflict_id.user_upload_id;

update data.valuation_function 
set endpoint_group_id = tmp_conflict_id.default_id
from tmp_conflict_id
where data.valuation_function.endpoint_group_id = tmp_conflict_id.user_upload_id;

--delete old endpoint_group
delete from data.endpoint_group where id in (select user_upload_id from tmp_conflict_id);

-----health impact function groups ----
--reuse tmp_conflict_id
delete from tmp_conflict_id;

--get health impact function group ids that have conflicts and store in a temp table
insert into tmp_conflict_id(user_upload_id, default_id)
select hifg2.id as user_upload_id, hifg1.id as default_id 
from data.health_impact_function_group hifg1
inner join data.health_impact_function_group hifg2 on lower(hifg1.name)=lower(hifg2."name") 
where coalesce(hifg1.user_id,'')='' and coalesce(hifg2.user_id,'')<>'';

--We don't want to merge functions uploaded by users to the default group we are adding therefore we are not updating health_impact_function_group_member
--The current solution is to rename the user-uploaded group to include user name. 
update data.health_impact_function_group  
set name = name || ' (' || user_id || ')'
where id in (select user_upload_id from tmp_conflict_id);
