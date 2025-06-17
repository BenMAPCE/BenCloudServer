/**** Mark 2010 grid definitions are archive ****/

UPDATE "data".settings SET value_int=58 WHERE "key"='version';

ALTER TABLE "data".grid_definition ADD archive int2 DEFAULT 0 NULL;
update "data".grid_definition gd set archive = 1 where id in (18,19,20);

VACUUM (VERBOSE, ANALYZE) "data".grid_definition;

