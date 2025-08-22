
UPDATE data.settings SET value_int=69 WHERE "key"='version';

ALTER TABLE "data".valuation_function ADD archived smallint NULL DEFAULT 0;

INSERT INTO "data".valuation_function_dataset (id, "name")	VALUES (1, 'User-uploaded functions');

SELECT SETVAL('data.timing_type_id_seq', COALESCE(MAX(id), 1) ) FROM data.timing_type;
