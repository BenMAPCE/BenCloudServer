/**** Add epa_standard column to HIF Groups ****/

UPDATE data.settings SET value_int=97 WHERE "key"='version';

ALTER TABLE "data".health_impact_function_group ADD epa_standard boolean NULL DEFAULT false;
UPDATE "data".health_impact_function_group SET epa_standard = true where name in 
('Premature Death - All',
 'Chronic Effects - All',
 'Acute Effects - All',
 'Premature Death - Primary',
 'Chronic Effects - Primary',
 'Acute Effects - Primary'
 );