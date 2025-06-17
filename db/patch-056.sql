/**** Add projection reference for CMAQ 12km Nation Straight Clip ****/

UPDATE "data".settings SET value_int=56 WHERE "key"='version';

SELECT UpdateGeometrySRID('grids','us_cmaq_12km_nation_straightclip','geom',4269);