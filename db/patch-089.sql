/*Add valuation_function.discounted variable */
UPDATE "data".settings SET value_int=89 where "key"='version';

ALTER TABLE "data".valuation_function ADD discounted text NULL;

UPDATE "data".valuation_function set discounted = 'false'; 
UPDATE "data".valuation_function set discounted = 'true' where qualifier like '%\% DR%' or qualifier like '%\% d.r.%';