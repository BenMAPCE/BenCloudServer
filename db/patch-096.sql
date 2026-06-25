/**** Add epa_standard column to incidence datasets and grid definitions ****/

UPDATE data.settings SET value_int=96 WHERE "key"='version';

ALTER TABLE "data".incidence_dataset ADD epa_standard boolean NULL DEFAULT false;
UPDATE "data".incidence_dataset SET epa_standard = true where share_scope = 1;

ALTER TABLE "data".grid_definition ADD epa_standard boolean NULL DEFAULT false;
UPDATE "data".grid_definition SET epa_standard = true where share_scope = 1;