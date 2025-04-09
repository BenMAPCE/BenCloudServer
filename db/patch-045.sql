/**** update settings for banner.****/

UPDATE "data".settings SET value_int=45 WHERE "key"='version';

--

ALTER TABLE "data".settings ADD status int4 NULL;
ALTER TABLE "data".settings ADD modified_by text NULL;
ALTER TABLE "data".settings ADD modified_date timestamp NULL;

--

INSERT INTO "data".settings ("key", value_text, value_int, status) VALUES('banner', null, 1, 0);