/*******************************************
Change default PM and Ozone AQS grids from CMAQ 12km Nation to CMAQ 12km Nation Straight Clip
Hide CMAQ 12km Nation grid from UI (keep in db) and rename CMAQ 12km Nation Straight Clip to "CMAQ 12km Nation" in the UI.
*******************************************/

UPDATE data.settings SET value_int=76 WHERE "key"='version';

update data.air_quality_layer set grid_definition_id = 77 where grid_definition_id = 28 and id in (6, 36, 16, 19, 23, 21);

update data.grid_definition set archive=1 where id = 28;
update data.grid_definition set name='CMAQ 12km Nation' where id = 77;