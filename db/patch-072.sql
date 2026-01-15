
UPDATE data.settings SET value_int=72 WHERE "key"='version';

UPDATE "data".health_impact_function SET archived = 1 WHERE endpoint_id = 139;
UPDATE "data".valuation_function SET archived = 1 WHERE endpoint_id = 139;