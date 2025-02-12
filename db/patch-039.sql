-- Rename grid definitions.
UPDATE "data".settings SET value_int=39 WHERE "key"='version';

UPDATE "data".population_dataset
	SET "name"='US CMAQ 12km Nation - 2020 census'
	WHERE id=50;
UPDATE "data".population_dataset
	SET "name"='US CMAQ 12km Nation - 2020 census centroid'
	WHERE id=51;
UPDATE "data".population_dataset
	SET "name"='US County - 2020 census'
	WHERE id=53;