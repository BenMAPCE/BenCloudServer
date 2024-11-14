-- 10/24/2024
-- Add file table
-- Add support for user-uploaded grid definitions

UPDATE "data".settings SET value_int=30 where "key"='version';

CREATE TABLE "data".file (
	id serial NOT NULL,
	filename text NULL,
	file_type text NULL,
	file_size bigint NULL,
	metadata text NULL,
	user_id text NULL,
	share_scope int2 NULL,
	created_date timestamp NULL,
	CONSTRAINT file_pk PRIMARY KEY (id)
);

ALTER TABLE "data".grid_definition ADD user_id text NULL;
ALTER TABLE "data".grid_definition ADD share_scope int2 DEFAULT 0 NULL;