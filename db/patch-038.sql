-- Add or replace index to crosswalk_entry table.
UPDATE "data".settings SET value_int=38 WHERE "key"='version';

DROP INDEX IF EXISTS data.crosswalk_entry_crosswalk_id_id_idx;
CREATE INDEX crosswalk_entry_crosswalk_id_id_idx ON data.crosswalk_entry USING btree (crosswalk_id);

VACUUM ANALYZE data.crosswalk_entry;