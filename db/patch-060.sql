-- Fix permissions issues

UPDATE "data".settings SET value_int=60 WHERE "key"='version';

GRANT CONNECT ON DATABASE benmap TO benmap_system;
GRANT USAGE ON SCHEMA data, grids, tiger, tiger_data, topology TO benmap_system;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA data, grids TO benmap_system;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA data, grids TO benmap_system;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA data, grids TO benmap_system;

-- Give system user full access to grids schema
grant all on all tables in schema grids to benmap_system;
grant all on schema grids to benmap_system;