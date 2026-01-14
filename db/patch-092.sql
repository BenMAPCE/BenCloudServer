/***Add new default AQ example layers (2 for PM, 2 for Ozone)***/
/***Delete old example 12-km air quality surfaces ***/
UPDATE "data".settings SET value_int=92 where "key"='version';


/*Add new default AQ example layers (2 for PM, 2 for Ozone)*/
--Import data into temp tables.
CREATE TABLE "data".ztmp_example_pm25_policy_surface (
	"Column" int4 NULL,
	"Row" int4 NULL,
	"Metric" varchar(50) NULL,
	"Seasonal Metric" varchar(50) NULL,
	"Annual Metric" varchar(50) NULL,
	"Value" float8 NULL
);
CREATE TABLE "data".ztmp_example_pm25_baseline_surface (
	"Column" int4 NULL,
	"Row" int4 NULL,
	"Metric" varchar(50) NULL,
	"Seasonal Metric" varchar(50) NULL,
	"Annual Metric" varchar(50) NULL,
	"Value" float8 NULL
);
CREATE TABLE "data".ztmp_example_ozone_policy_surface (
	"Column" int4 NULL,
	"Row" int4 NULL,
	"Metric" varchar(50) NULL,
	"Seasonal Metric" varchar(50) NULL,
	"Annual Metric" varchar(50) NULL,
	"Value" float8 NULL
);
CREATE TABLE "data".ztmp_example_ozone_baseline_surface (
	"Column" int4 NULL,
	"Row" int4 NULL,
	"Metric" varchar(50) NULL,
	"Seasonal Metric" varchar(50) NULL,
	"Annual Metric" varchar(50) NULL,
	"Value" float8 NULL
);
INSERT INTO "data".ztmp_example_pm25_policy_surface ("Column","Row","Metric","Seasonal Metric","Annual Metric","Value") VALUES
	 (1,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (2,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (4,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (5,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (6,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (8,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (9,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (10,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (11,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (12,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (13,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (15,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (16,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (17,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (18,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (19,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (20,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (21,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (22,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (23,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (24,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (25,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (26,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (27,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (28,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (29,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (30,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (31,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (32,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (33,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (34,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (35,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (36,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (37,1,'D24HourMean','QuarterlyMean','Mean',8),
	 (38,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (39,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (40,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (41,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (42,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (44,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (45,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (46,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (47,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (48,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (49,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (50,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (51,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (53,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (54,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (55,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (56,1,'D24HourMean','QuarterlyMean','Mean',9);

INSERT INTO "data".ztmp_example_pm25_baseline_surface ("Column","Row","Metric","Seasonal Metric","Annual Metric","Value") VALUES
	 (1,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (2,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (4,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (5,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (6,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (8,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (9,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (10,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (11,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (12,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (13,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (15,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (16,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (17,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (18,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (19,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (20,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (21,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (22,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (23,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (24,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (25,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (26,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (27,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (28,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (29,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (30,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (31,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (32,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (33,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (34,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (35,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (36,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (37,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (38,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (39,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (40,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (41,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (42,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (44,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (45,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (46,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (47,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (48,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (49,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (50,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (51,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (53,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (54,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (55,1,'D24HourMean','QuarterlyMean','Mean',9),
	 (56,1,'D24HourMean','QuarterlyMean','Mean',9);

INSERT INTO "data".ztmp_example_ozone_policy_surface ("Column","Row","Metric","Seasonal Metric","Annual Metric","Value") VALUES
	 (1,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (2,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (4,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (5,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (6,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (8,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (9,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (10,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (11,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (12,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (13,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (15,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (16,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (17,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (18,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (19,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (20,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (21,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (22,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (23,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (24,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (25,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (26,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (27,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (28,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (29,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (30,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (31,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (32,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (33,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (34,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (35,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (36,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (37,1,'D8HourMax','WarmSeason_D8HourMax','Mean',45),
	 (38,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (39,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (40,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (41,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (42,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (44,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (45,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (46,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (47,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (48,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (49,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (50,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (51,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (53,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (54,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (55,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (56,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50);

INSERT INTO "data".ztmp_example_ozone_baseline_surface ("Column","Row","Metric","Seasonal Metric","Annual Metric","Value") VALUES
	 (1,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (2,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (4,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (5,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (6,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (8,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (9,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (10,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (11,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (12,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (13,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (15,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (16,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (17,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (18,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (19,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (20,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (21,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (22,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (23,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (24,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (25,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (26,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (27,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (28,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (29,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (30,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (31,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (32,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (33,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (34,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (35,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (36,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (37,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (38,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (39,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (40,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (41,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (42,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (44,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (45,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (46,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (47,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (48,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (49,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (50,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (51,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (53,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (54,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (55,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50),
	 (56,1,'D8HourMax','WarmSeason_D8HourMax','Mean',50);


---insert into database tables
INSERT INTO "data".air_quality_layer ("name",pollutant_id,grid_definition_id,share_scope,aq_year,description,"source",data_type,filename,upload_date)
	VALUES ('Example PM2.5 Baseline Surface'
	,6
	,69,1,'2025'
	,'Example baseline air quality surface for projected annual 24-hour average PM2.5 concentrations in 2025'
	,'N/A','Photochemical AQ Model'
	,'Example PM25 Baseline Surface.csv'
	,'2026-01-14 14:22:49.996');
INSERT INTO "data".air_quality_layer ("name",pollutant_id,grid_definition_id,share_scope,aq_year,description,"source",data_type,filename,upload_date)
	VALUES ('Example PM2.5 Policy Surface'
	,6
	,69,1,'2025'
	,'Example policy air quality surface for projected annual 24-hour average PM2.5 concentrations in 2025'
	,'N/A','Photochemical AQ Model'
	,'Example PM25 Policy Surface.csv'
	,'2026-01-14 14:22:49.996');
INSERT INTO "data".air_quality_layer ("name",pollutant_id,grid_definition_id,share_scope,aq_year,description,"source",data_type,filename,upload_date)
	VALUES ('Example Ozone Baseline Surface'
	,4
	,69,1,'2025'
	,'Example baseline air quality surface for projected annual 8-hour max ozone concentrations in 2025'
	,'N/A','Photochemical AQ Model'
	,'Example Ozone Baseline Surface.csv'
	,'2026-01-14 14:22:49.996');
INSERT INTO "data".air_quality_layer ("name",pollutant_id,grid_definition_id,share_scope,aq_year,description,"source",data_type,filename,upload_date)
	VALUES ('Example Ozone Policy Surface'
	,4
	,69,1,'2025'
	,'Example policy air quality surface for projected annual 8-hour max ozone concentrations in 2025'
	,'N/A','Photochemical AQ Model'
	,'Example Ozone Policy Surface.csv'
	,'2026-01-14 14:22:49.996');

--Insert into aq cells table
insert into data.air_quality_cell(air_quality_layer_id, grid_col, grid_row, grid_cell_id, metric_id, seasonal_metric_id, annual_statistic_id, value)
select aql.id as air_quality_layer_id,tmp."Column" as grid_col, tmp."Row" as grid_row
, ((tmp."Column"::BIGINT+tmp."Row"::BIGINT)*(tmp."Column"::BIGINT+tmp."Row"::BIGINT+1)*0.5)+tmp."Row"::BIGINT as grid_cell_id
, pm.id as metric_id
, sm.id as seasonal_metric_id
, st.id as annual_statistic_id
, tmp."Value" as value
from 
data.ztmp_example_pm25_baseline_surface tmp 
inner join data.pollutant_metric pm on tmp."Metric" = pm."name" and pm.pollutant_id  = 6
inner join data.seasonal_metric sm on tmp."Seasonal Metric" = sm."name" and pm.id = sm.metric_id 
inner join data.statistic_type st on tmp."Annual Metric" = st."name" 
cross join data.air_quality_layer aql
where aql."name"  = 'Example PM2.5 Baseline Surface' and aql.user_id is null;

insert into data.air_quality_cell(air_quality_layer_id, grid_col, grid_row, grid_cell_id, metric_id, seasonal_metric_id, annual_statistic_id, value)
select aql.id as air_quality_layer_id,tmp."Column" as grid_col, tmp."Row" as grid_row
, ((tmp."Column"::BIGINT+tmp."Row"::BIGINT)*(tmp."Column"::BIGINT+tmp."Row"::BIGINT+1)*0.5)+tmp."Row"::BIGINT as grid_cell_id
, pm.id as metric_id
, sm.id as seasonal_metric_id
, st.id as annual_statistic_id
, tmp."Value" as value
from 
"data".ztmp_example_pm25_policy_surface tmp 
inner join data.pollutant_metric pm on tmp."Metric" = pm."name" and pm.pollutant_id  = 6
inner join data.seasonal_metric sm on tmp."Seasonal Metric" = sm."name" and pm.id = sm.metric_id 
inner join data.statistic_type st on tmp."Annual Metric" = st."name" 
cross join data.air_quality_layer aql
where aql."name"  = 'Example PM2.5 Policy Surface' and aql.user_id is null; 

insert into data.air_quality_cell(air_quality_layer_id, grid_col, grid_row, grid_cell_id, metric_id, seasonal_metric_id, annual_statistic_id, value)
select aql.id as air_quality_layer_id,tmp."Column" as grid_col, tmp."Row" as grid_row
, ((tmp."Column"::BIGINT+tmp."Row"::BIGINT)*(tmp."Column"::BIGINT+tmp."Row"::BIGINT+1)*0.5)+tmp."Row"::BIGINT as grid_cell_id
, pm.id as metric_id
, sm.id as seasonal_metric_id
, st.id as annual_statistic_id
, tmp."Value" as value
from 
"data".ztmp_example_ozone_baseline_surface tmp 
inner join data.pollutant_metric pm on tmp."Metric" = pm."name" and pm.pollutant_id  = 4
inner join data.seasonal_metric sm on tmp."Seasonal Metric" = sm."name" and pm.id = sm.metric_id 
inner join data.statistic_type st on tmp."Annual Metric" = st."name" 
cross join data.air_quality_layer aql
where aql."name"  = 'Example Ozone Baseline Surface' and aql.user_id is null;

insert into data.air_quality_cell(air_quality_layer_id, grid_col, grid_row, grid_cell_id, metric_id, seasonal_metric_id, annual_statistic_id, value)
select aql.id as air_quality_layer_id,tmp."Column" as grid_col, tmp."Row" as grid_row
, ((tmp."Column"::BIGINT+tmp."Row"::BIGINT)*(tmp."Column"::BIGINT+tmp."Row"::BIGINT+1)*0.5)+tmp."Row"::BIGINT as grid_cell_id
, pm.id as metric_id
, sm.id as seasonal_metric_id
, st.id as annual_statistic_id
, tmp."Value" as value
from 
"data".ztmp_example_ozone_policy_surface tmp 
inner join data.pollutant_metric pm on tmp."Metric" = pm."name" and pm.pollutant_id  = 4
inner join data.seasonal_metric sm on tmp."Seasonal Metric" = sm."name" and pm.id = sm.metric_id 
inner join data.statistic_type st on tmp."Annual Metric" = st."name" 
cross join data.air_quality_layer aql
where aql."name"  = 'Example Ozone Policy Surface' and aql.user_id is null;


--aq layer metrics table
insert into data.air_quality_layer_metrics (air_quality_layer_id, metric_id, seasonal_metric_id, annual_statistic_id
, cell_count, min_value, max_value, mean_value, pct_2_5, pct_97_5)
select aqc.air_quality_layer_id, aqc.metric_id, aqc.seasonal_metric_id, aqc.annual_statistic_id
, count(1) as cell_count
, min(value) as min_value, max(value) as max_value, avg(value) as mean_value
, percentile_cont(0.025) WITHIN GROUP (ORDER BY value) as pct_2_5
, percentile_cont(0.975) WITHIN GROUP (ORDER BY value) as pct_97_5
from data.air_quality_cell aqc 
inner join data.air_quality_layer aql on aqc.air_quality_layer_id = aql.id 
where aql.name in ('Example PM2.5 Baseline Surface' ,'Example PM2.5 Policy Surface','Example Ozone Baseline Surface','Example Ozone Policy Surface')
 and aql.user_id is null
group by aqc.air_quality_layer_id, aqc.metric_id, aqc.seasonal_metric_id, aqc.annual_statistic_id ;

--remove temp table
drop table "data".ztmp_example_ozone_baseline_surface;
drop table "data".ztmp_example_ozone_policy_surface;
drop table "data".ztmp_example_pm25_baseline_surface;
drop table "data".ztmp_example_pm25_policy_surface;





/*Delete old example 12-km air quality surfaces */
--Delete old 12-km air quality surfaces from the database
delete from data.air_quality_cell where air_quality_layer_id in (select id from data.air_quality_layer aql where aql."name" in ('Ozone Baseline Example 2023'
,'Ozone Policy Example 2023'
,'PM Policy Example 2 2032'
,'PM Policy Example 3 2032')
 and aql.user_id is null);
 
 delete from data.air_quality_layer_metrics  where air_quality_layer_id in (select id from data.air_quality_layer aql where aql."name" in ('Ozone Baseline Example 2023'
,'Ozone Policy Example 2023'
,'PM Policy Example 2 2032'
,'PM Policy Example 3 2032')
 and aql.user_id is null);
 
delete from data.air_quality_layer aql where aql."name" in ('Ozone Baseline Example 2023'
,'Ozone Policy Example 2023'
,'PM Policy Example 2 2032'
,'PM Policy Example 3 2032')
 and aql.user_id is null

 --Rename “PM Baseline Example 2032” to “PM2.5 Example A,” change the Source from “EPA” to “N/A,” 
 update data.air_quality_layer
 set name = 'PM2.5 Example A', source = 'N/A', description = 'Example air quality surface for projected annual 24-hour average PM2.5 concentrations in 2032.'
 where name = 'PM Baseline Example 2032' and user_id is null;
 
  update data.air_quality_layer
 set name = 'PM2.5 Example B', source = 'N/A', description = 'Example air quality surface for projected annual 24-hour average PM2.5 concentrations in 2032.'
 where name = 'PM Policy Example 1 2032' and user_id is null;