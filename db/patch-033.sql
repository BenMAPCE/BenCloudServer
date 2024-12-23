-- Adding support for shapefile upload

UPDATE "data".settings SET value_int=33 WHERE "key"='version';

-- Give system user full access to grids schema
grant all on all tables in schema grids to benmap_system;
grant all on schema grids to benmap_system;

-- Make EPA-provided grids shared so that they cannot be deleted
UPDATE "data".grid_definition SET share_scope=1 WHERE id in (18,19,20,28);

-- Store task log in grid_definition table
ALTER TABLE "data".grid_definition ADD task_log json NULL;
