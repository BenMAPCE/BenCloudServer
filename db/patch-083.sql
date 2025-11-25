/*create EPA default incidence dataset "State" and add dementia incidence rates*/

UPDATE "data".settings SET value_int=83 where "key"='version';

--create a temp table
CREATE TEMPORARY TABLE tmp_new_incidence (
    id INT
);

--add new incidence dataset and store id in the temp table
with new_incidence_dataset_id as (
	--add incidence dataset	
	insert into data.incidence_dataset ( "name", grid_definition_id, share_scope, upload_date)
	select 'State', 69, 1, now()
	where not exists(select 1 from data.incidence_dataset where name = 'State' and grid_definition_id = 1)
	returning id
)
insert into tmp_new_incidence select id from new_incidence_dataset_id;

--use the temp table to update incidence_entry. (also to correct value in prevalence column)
update data.incidence_entry
set incidence_dataset_id = newId.id, prevalence = false
from tmp_new_incidence newId
where endpoint_id in (
select e.id from data.endpoint e 
inner join data.endpoint_group eg on e.endpoint_group_id = eg.id 
where e."name" = 'Incidence, Dementia' and eg."name" = 'Incidence, Neurological' and coalesce(eg.user_id ,'')=''
);

--select * from data.incidence_dataset id inner join data.incidence_entry ie on id.id = ie.incidence_dataset_id inner join data.endpoint e on ie.endpoint_id = e.id where e.name = 'Incidence, Dementia'






