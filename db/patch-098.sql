/**** Update name of archived, unclipped 12km grid ****/

UPDATE data.settings SET value_int=98 WHERE "key"='version';

UPDATE data.grid_definition SET name = 'CMAQ 12km Nation (unclipped)'
  WHERE id = 28;