-- 12/19/2023
-- BCD-282 - Add epa_standard flag to valuation_function

UPDATE "data".settings SET value_int=23 where "key"='version';

ALTER TABLE "data".valuation_function ADD epa_standard boolean NOT NULL DEFAULT false;

vacuum analyze;