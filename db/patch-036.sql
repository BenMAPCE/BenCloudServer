-- make adjustments in the grid_definition table.

UPDATE "data".settings SET value_int=36 WHERE "key"='version';

UPDATE "data".grid_definition
	SET share_scope=1
	WHERE id=68;
UPDATE "data".grid_definition
	SET share_scope=1,table_name='grids.us_state'
	WHERE id=69;
UPDATE "data".grid_definition
	SET share_scope=1,table_name='grids.us_nation'
	WHERE id=70;
UPDATE "data".grid_definition
	SET share_scope=1
	WHERE id=83;
