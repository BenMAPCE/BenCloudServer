/**** Update HIF qualifier's for Barry et al. functions to differentiate valuation results ***/

UPDATE "data".settings SET value_int=53 WHERE "key"='version';

update data.health_impact_function set qualifier = 'All year. City-specific rate ratios, St. Louis, RD, lag 0-2 days (Table 3)' where id = 874;
update data.health_impact_function set qualifier = 'All year. City-specific rate ratios, Atlanta, RD, lag 0-2 days (Table 3)' where id = 875;
update data.health_impact_function set qualifier = 'All year. City-specific rate ratios, Birmingham, RD, lag 0-2 days (Table 3)' where id = 876;
update data.health_impact_function set qualifier = 'All year. City-specific rate ratios, Pittsburgh, RD, lag 0-2 days (Table 3)', location = 'Pittsburgh, PA' where id = 877;
update data.health_impact_function set qualifier = 'All year. City-specific rate ratios, Dallas, RD, lag 0-2 days (Table 3)' where id = 878;


update data.valuation_function set epa_standard = true where id = 498;
update data.valuation_function set epa_standard = false where id = 499;