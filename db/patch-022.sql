-- 12/13/2023
-- Correct age range issue with Older Adults exposure category

UPDATE "data".settings SET value_int=22 where "key"='version';

UPDATE "data".exposure_function SET population_group='Older Adults (65-99)',start_age=65 WHERE id=12;

vacuum analyze;