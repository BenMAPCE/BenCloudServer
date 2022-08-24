-- Set start age for Wei et al. HIF to 65

UPDATE "data".settings SET value_int=6 where "key"='version';

update data.health_impact_function h
set start_age = 65
where id = 1018;
