GRANT CONNECT ON DATABASE benmap TO benmap_system;
GRANT USAGE ON SCHEMA data, grids, tiger, tiger_data, topology TO benmap_system;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA data, grids TO benmap_system;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA data, grids TO benmap_system;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA data, grids TO benmap_system;