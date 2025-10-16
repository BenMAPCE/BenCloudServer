
UPDATE data.settings SET value_int=71 WHERE "key"='version';

ALTER TABLE "data".health_impact_function ADD geographic_area text NULL;
ALTER TABLE "data".health_impact_function ADD geographic_area_feature text NULL;