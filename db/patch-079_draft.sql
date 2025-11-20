/*Add New Health Effects Dementia with new hif, vf, and incidence from BWD-223*/

UPDATE data.settings SET value_int=79 WHERE "key"='version';


--- Create new endpoint group and endpoint for 'Incidence, Dementia' under 'Incidence, Neurological'
----- Check if the endpoint group 'Incidence, Neurological' exists; if not, create it
----- Check if the endpoint 'Incidence, Dementia' exists; if not, create it
----- Return the ids of the created or existing endpoint group and endpoint

CREATE TABLE data.tmp_table (
    ed_group_id INTEGER,
    ed_id INTEGER
);


WITH existing_ed_group AS (
    SELECT id FROM data.endpoint_group WHERE name = 'Incidence, Neurological'
),
new_ed_group AS (
    INSERT INTO data.endpoint_group (name, share_scope)
    SELECT 'Incidence, Neurological', 1
    WHERE NOT EXISTS (SELECT 1 FROM existing_ed_group)
    RETURNING id
),
final_ed_group AS (
    SELECT id FROM new_ed_group
    UNION
    SELECT id FROM existing_ed_group
),
existing_ed AS (
    SELECT id FROM data.endpoint WHERE name = 'Incidence, Dementia'
),
new_ed AS (
    INSERT INTO data.endpoint (name, endpoint_group_id)
    SELECT 'Incidence, Dementia', id FROM final_ed_group
    WHERE NOT EXISTS (SELECT 1 FROM existing_ed)
    RETURNING id
),
final_ed AS (
    SELECT id FROM new_ed
    UNION
    SELECT id FROM existing_ed
)
INSERT INTO data.tmp_table (ed_group_id, ed_id)
SELECT fg.id, fe.id
FROM final_ed_group fg, final_ed fe
RETURNING ed_group_id, ed_id;


--# CREATE HEALTH IMPACT FUNCTIONS 
--# Add one new entry to health impact function table for Dementia incidence based on Wilker et al. 2023
CREATE TABLE data.tmp_dem_hif (
"health_impact_function_dataset_id" int4 NULL, "pollutant_id" int4 NULL, "metric_id" int4 NULL, "seasonal_metric_id" int4 NULL, "metric_statistic"int4 NULL,"author" text  NULL,
"function_year" int4 NULL, "location" text NULL, "other_pollutants" text NULL, "qualifier" text NULL, "reference" text NULL, "start_age" int4 NULL, "end_age" int4 NULL,"function_text" text NULL,
"beta" float8 NULL, "dist_beta" text NULL, "p1_beta" float8 NULL, "p2_beta" float8 NULL, "val_a" float8 NULL, "name_a" text NULL, "val_b" float8 NULL, "name_b" text NULL,"val_c" float8 NULL,
"name_c" text NULL, "baseline_function_text" text NULL, "race_id" int4 NULL, "gender_id" int4 NULL, "ethnicity_id" int4 NULL, "start_day" int4 NULL, "end_day"int4 NULL,"hero_id"int4 NULL,
"epa_hero_url" text NULL, "access_url" text NULL, "user_id" text NULL, "share_scope" int2 NULL, "archived" int2 NULL,  "timing_id" int2 NULL, "geographic_area" text NULL,"geographic_area_feature" text NULL
);

insert into data.tmp_dem_hif(
"health_impact_function_dataset_id", "pollutant_id", "metric_id", "seasonal_metric_id"  , "metric_statistic" ,"author"   ,
"function_year"  , "location"  , "other_pollutants"  , "qualifier"  , "reference"  , "start_age"  , "end_age"  ,"function_text"  ,
"beta"  , "dist_beta"  , "p1_beta"  , "p2_beta"  , "val_a"  , "name_a"  , "val_b"  , "name_b"  ,"val_c"  ,
"name_c"  , "baseline_function_text"  , "race_id"  , "gender_id"  , "ethnicity_id"  , "start_day"  , "end_day" ,"hero_id" ,
"epa_hero_url"  , "access_url"  , "user_id"  , "share_scope"  , "archived"  ,  "timing_id"  , "geographic_area"  ,"geographic_area_feature"  
) VALUES
(15, 6, 11, 8, 1, 'Wilker et al.', 2023, 'Nationwide', NULL, 'Pooled random-effect estimate using North American active ascertainment studies (Semmens 2022; Shaffer 2021; Sullivan 2021; Wang 2022), HR (95% CI) per 2 ug/m3, Dementia disease', 
    'Wilker, E. H., Osman, M., & Weisskopf, M. G. (2023). Ambient air pollution and clinical dementia: systematic review and meta-analysis. bmj, 381.', 55, 99,
    '(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION', 0.142589471, 'Normal', 0.140908265, 0,0, NULL, 0, NULL, 0, NULL, 'INCIDENCE*POPULATION', 5, 3, 3, NULL, NULL,11175807,
    'https://hero.epa.gov/hero/index.cfm/reference/details/reference_id/11175807', 'https://www.bmj.com/content/381/bmj-2022-071620', NULL, 1, 0, 1, NULL, NULL
);

insert into data.health_impact_function ("health_impact_function_dataset_id","endpoint_group_id","endpoint_id", "pollutant_id", "metric_id", 
"seasonal_metric_id", "metric_statistic","author", "function_year", "location", "other_pollutants", "qualifier", "reference", "start_age",
"end_age","function_text","beta", "dist_beta", "p1_beta", "p2_beta", "val_a", "name_a", "val_b" , "name_b" ,"val_c", "name_c", "baseline_function_text" ,
"race_id" , "gender_id" , "ethnicity_id" , "start_day" , "end_day","hero_id", "epa_hero_url", "access_url" , "user_id" , "share_scope", "archived",
"timing_id" , "geographic_area","geographic_area_feature")
select tdh.health_impact_function_dataset_id, tt.ed_group_id as endpoint_group_id, tt.ed_id as endpoint_id, tdh.pollutant_id, tdh.metric_id, tdh.seasonal_metric_id
	, tdh.metric_statistic, tdh.author, tdh.function_year, tdh.location, tdh.other_pollutants, tdh.qualifier, tdh.reference, tdh.start_age, tdh.end_age, 
	tdh.function_text, tdh.beta, tdh.dist_beta, tdh.p1_beta, tdh.p2_beta, tdh.val_a, tdh.name_a, tdh.val_b, tdh.name_b, tdh.val_c, tdh.name_c, 
	tdh.baseline_function_text, tdh.race_id, tdh.gender_id, tdh.ethnicity_id, tdh.start_day, tdh.end_day, tdh.hero_id, tdh.epa_hero_url, tdh.access_url,
	tdh.user_id, tdh.share_scope, tdh.archived, tdh.timing_id, tdh.geographic_area, tdh.geographic_area_feature
from  data.tmp_table tt 
cross join data.tmp_dem_hif tdh;

--# CREATE VALUATION FUNCTIONS 
--# Add 3 new valuation functions for Dementia 
CREATE table data.tmp_dem_vf("valuation_dataset_id" int4 NULL, "qualifier" text NULL,"reference"text NULL,"start_age"int4 NULL,"end_age"int4 NULL,"function_text" text NULL,"val_a" float8 NULL,"name_a" text NULL,"dist_a" text NULL,"p1a" float8 NULL,
"p2a" float8 NULL,"val_b" float8 NULL,"name_b" text NULL,"val_c" float8 NULL,"name_c" text NULL,"val_d" float8 NULL,"name_d" text NULL,"epa_standard" bool NULL, "access_url" text NULL, "valuation_type" text NULL ,"multiyear" bool NULL,
 "multiyear_dr" float8 NULL,"multiyear_costs" _float8 NULL,"user_id" text NULL, "share_scope" int2 null , "archived" int2 NULL);

Insert into data.tmp_dem_vf(valuation_dataset_id,qualifier,reference,start_age,end_age,function_text,val_a,name_a,dist_a,p1a,p2a,val_b,name_b,val_c,name_c,val_d,name_d,epa_standard,access_url,valuation_type,multiyear,multiyear_dr,multiyear_costs,user_id,share_scope,archived) VALUES
(8,'COI: 4.8 yrs med + informal care (replacement cost), 2% DR','Medical costs: Nandi, A., Counts, N., Bröker, J., Malik, S., Chen, S., Han, R., ... & Bloom, D. E. (2024). Cost of care for Alzheimer’s disease and related dementias in the United States: 2016 to 2060. npj Aging, 10(1), 13; Liang, C. S., Li, D. J., Yang, F. C., Tseng, P. T., Carvalho, A. F., Stubbs, B., ... & Chu, C. S. (2021). Years of survival from diagnosis: Mortality rates in Alzheimer’s disease and non-Alzheimer’s dementias: a systematic review and meta-analysis. The Lancet Healthy Longevity, 2(8), e479-e488.',70,99,'A*MedicalCostIndex+B*WageIndex',120014.49,'Formal medical cost (from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 2% DR','None',0,0,156726.66,'Informal care (replacement cost, from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 2% DR',0,0,0,0,TRUE,'https://www.nature.com/articles/s41514-024-00136-6',NULL,FALSE,NULL,NULL,NULL,1,0),
(8,'COI: 4.8 yrs med + informal care (replacement cost), 3% DR','Medical costs: Nandi, A., Counts, N., Bröker, J., Malik, S., Chen, S., Han, R., ... & Bloom, D. E. (2024). Cost of care for Alzheimer’s disease and related dementias in the United States: 2016 to 2060. npj Aging, 10(1), 13; Liang, C. S., Li, D. J., Yang, F. C., Tseng, P. T., Carvalho, A. F., Stubbs, B., ... & Chu, C. S. (2021). Years of survival from diagnosis: Mortality rates in Alzheimer’s disease and non-Alzheimer’s dementias: a systematic review and meta-analysis. The Lancet Healthy Longevity, 2(8), e479-e488.',70,99,'A*MedicalCostIndex+B*WageIndex',118580.51,'Formal medical cost (from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 3% DR','None',0,0,153939.09,'Informal care (replacement cost, from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 3% DR',0,0,0,0,TRUE,'https://www.nature.com/articles/s41514-024-00136-6',NULL,FALSE,NULL,NULL,NULL,1,0),
(8,'COI: 4.8 yrs med + informal care (replacement cost), 7% DR','Medical costs: Nandi, A., Counts, N., Bröker, J., Malik, S., Chen, S., Han, R., ... & Bloom, D. E. (2024). Cost of care for Alzheimer’s disease and related dementias in the United States: 2016 to 2060. npj Aging, 10(1), 13; Liang, C. S., Li, D. J., Yang, F. C., Tseng, P. T., Carvalho, A. F., Stubbs, B., ... & Chu, C. S. (2021). Years of survival from diagnosis: Mortality rates in Alzheimer’s disease and non-Alzheimer’s dementias: a systematic review and meta-analysis. The Lancet Healthy Longevity, 2(8), e479-e488.',70,99,'A*MedicalCostIndex+B*WageIndex',113280.26,'Formal medical cost (from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 7% DR','None',0,0,143752.35,'Informal care (replacement cost, from Nandi et al. 2024 Table 1) over 4.8 years (survival from diagnosis from Liang et al. 2021 Table 1) in 2015$, 7% DR',0,0,0,0,TRUE,'https://www.nature.com/articles/s41514-024-00136-6',NULL,FALSE,NULL,NULL,NULL,1,0);

insert into data.valuation_function ("valuation_dataset_id","endpoint_group_id","endpoint_id","qualifier","reference","start_age","end_age","function_text","val_a","name_a","dist_a","p1a","p2a","val_b","name_b","val_c","name_c",
"val_d","name_d","epa_standard","access_url","valuation_type","multiyear","multiyear_dr","multiyear_costs","user_id","share_scope","archived")
select vf.valuation_dataset_id, tt.ed_group_id, tt.ed_id, vf.qualifier, vf.reference, vf.start_age, vf.end_age,vf.function_text,vf.val_a,vf.name_a,vf.dist_a,vf.p1a,vf.p2a,vf.val_b,vf.name_b,vf.val_c,vf.name_c,vf.val_d,
vf.name_d,vf.epa_standard,vf.access_url,vf.valuation_type,vf.multiyear,vf.multiyear_dr,vf.multiyear_costs,vf.user_id,vf.share_scope,vf.archived
from data.tmp_table tt 
cross join data.tmp_dem_vf vf;

--# Create incidence INFORMATION
--# add incidence information for Dementia
--# add incidence_entry, get new ids, then enter into incidence_values table

CREATE TABLE "data".tmp_otherIncidence_dementia (
    "Column" int4 NULL,
	"Row" int4 NULL,
	"Endpoint Group" text NULL,
    "Endpoint" text NULL,
    "Race" varchar(50) NULL,
	"Gender" varchar(50) NULL,
	"Ethnicity" varchar(50) NULL,
    "StartAge" int4 NULL,
	"EndAge" int4 NULL,
	"Type" varchar(50) NULL,
    "Timeframe" varchar(50) NULL,
	"Units" varchar(50) NULL,    
    "Value" float4 NULL,
	"Distribution" varchar(50) NULL,
	"StandardError" varchar(50) NULL,
    "Year" int4 NULL
);

insert into data.tmp_otherIncidence_dementia("Column","Row","Endpoint Group","Endpoint","Race","Gender","Ethnicity","Year","StartAge","EndAge","Type","Timeframe","Units","Value","Distribution","StandardError")Values
(1,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0057,NULL,NULL),
(1,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0114,NULL,NULL),
(1,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0184,NULL,NULL),
(1,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0219,NULL,NULL),
(2,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0047,NULL,NULL),
(2,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0101,NULL,NULL),
(2,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(2,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0195,NULL,NULL),
(4,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0044,NULL,NULL),
(4,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0096,NULL,NULL),
(4,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0161,NULL,NULL),
(4,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0188,NULL,NULL),
(5,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0051,NULL,NULL),
(5,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0105,NULL,NULL),
(5,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0175,NULL,NULL),
(5,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0194,NULL,NULL),
(6,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0044,NULL,NULL),
(6,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0096,NULL,NULL),
(6,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0161,NULL,NULL),
(6,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0188,NULL,NULL),
(8,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(8,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(8,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(8,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL),
(9,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(9,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(9,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(9,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(10,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(10,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(10,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(10,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(11,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(11,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(11,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(11,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(12,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.005,NULL,NULL),
(12,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0098,NULL,NULL),
(12,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0164,NULL,NULL),
(12,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.019,NULL,NULL),
(13,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.005,NULL,NULL),
(13,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0098,NULL,NULL),
(13,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0164,NULL,NULL),
(13,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.019,NULL,NULL),
(15,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0044,NULL,NULL),
(15,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0096,NULL,NULL),
(15,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0161,NULL,NULL),
(15,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0188,NULL,NULL),
(16,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0047,NULL,NULL),
(16,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0101,NULL,NULL),
(16,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(16,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0195,NULL,NULL),
(17,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(17,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(17,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(17,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(18,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(18,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(18,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(18,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(19,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0052,NULL,NULL),
(19,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0099,NULL,NULL),
(19,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0158,NULL,NULL),
(19,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(20,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0052,NULL,NULL),
(20,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0099,NULL,NULL),
(20,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0158,NULL,NULL),
(20,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(21,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0057,NULL,NULL),
(21,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0114,NULL,NULL),
(21,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0184,NULL,NULL),
(21,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0219,NULL,NULL),
(22,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0051,NULL,NULL),
(22,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0105,NULL,NULL),
(22,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0175,NULL,NULL),
(22,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0194,NULL,NULL),
(23,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(23,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(23,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(23,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(24,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(24,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(24,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(24,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(25,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(25,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(25,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(25,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(26,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(26,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(26,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(26,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(27,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(27,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(27,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(27,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(28,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0057,NULL,NULL),
(28,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0114,NULL,NULL),
(28,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0184,NULL,NULL),
(28,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0219,NULL,NULL),
(29,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0052,NULL,NULL),
(29,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0099,NULL,NULL),
(29,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0158,NULL,NULL),
(29,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(30,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(30,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(30,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(30,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL),
(31,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0052,NULL,NULL),
(31,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0099,NULL,NULL),
(31,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0158,NULL,NULL),
(31,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(32,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0044,NULL,NULL),
(32,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0096,NULL,NULL),
(32,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0161,NULL,NULL),
(32,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0188,NULL,NULL),
(33,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(33,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(33,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(33,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(34,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(34,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(34,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(34,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(35,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0051,NULL,NULL),
(35,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0105,NULL,NULL),
(35,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0175,NULL,NULL),
(35,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0194,NULL,NULL),
(36,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(36,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(36,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(36,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(37,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.005,NULL,NULL),
(37,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0098,NULL,NULL),
(37,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0164,NULL,NULL),
(37,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.019,NULL,NULL),
(38,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(38,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(38,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(38,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL),
(39,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(39,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(39,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(39,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(40,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0051,NULL,NULL),
(40,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0105,NULL,NULL),
(40,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0175,NULL,NULL),
(40,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0194,NULL,NULL),
(41,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0047,NULL,NULL),
(41,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0101,NULL,NULL),
(41,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(41,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0195,NULL,NULL),
(42,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(42,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(42,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(42,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(44,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(44,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(44,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(44,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(45,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.005,NULL,NULL),
(45,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0098,NULL,NULL),
(45,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0164,NULL,NULL),
(45,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.019,NULL,NULL),
(46,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(46,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(46,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(46,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL),
(47,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0057,NULL,NULL),
(47,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0114,NULL,NULL),
(47,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0184,NULL,NULL),
(47,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0219,NULL,NULL),
(48,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0051,NULL,NULL),
(48,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0105,NULL,NULL),
(48,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0175,NULL,NULL),
(48,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0194,NULL,NULL),
(49,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(49,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(49,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(49,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL),
(50,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(50,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(50,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0152,NULL,NULL),
(50,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0171,NULL,NULL),
(51,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(51,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(51,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(51,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(53,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0047,NULL,NULL),
(53,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0101,NULL,NULL),
(53,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(53,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0195,NULL,NULL),
(54,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(54,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0091,NULL,NULL),
(54,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0143,NULL,NULL),
(54,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0177,NULL,NULL),
(55,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0046,NULL,NULL),
(55,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0095,NULL,NULL),
(55,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0156,NULL,NULL),
(55,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0172,NULL,NULL),
(56,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,55,64,'Incidence ','Annual','Cases/person-years',0.0045,NULL,NULL),
(56,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,65,74,'Incidence ','Annual','Cases/person-years',0.0102,NULL,NULL),
(56,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,75,84,'Incidence ','Annual','Cases/person-years',0.0178,NULL,NULL),
(56,1,'Incidence, Neurological','Incidence, Dementia',NULL,NULL,NULL,2015,85,99,'Incidence ','Annual','Cases/person-years',0.0183,NULL,NULL);


insert into data.incidence_entry( incidence_dataset_id, "year", endpoint_group_id, endpoint_id, race_id, gender_id, start_age, end_age, prevalence, ethnicity_id, timeframe, units, distribution )
select distinct 3 incidence_dataset_id
, tmp."Year" as "year", eg.id as endpoint_group_id, e2.id as endpoint_id, r.id as race_id, g.id as gender_id, tmp."StartAge" as start_age, tmp."EndAge" as end_age
, case when lower(tmp."Type")='incidence' then false else true end as prevalence, e.id as ethnicity_id, null as timeframe, null as units, null as distribution  
from data.tmp_otherIncidence_dementia tmp 
left join data.race r on coalesce(tmp."Race",'') =coalesce(r."name",'')
left join data.gender g on coalesce(tmp."Gender",'') = coalesce(g."name",'')
left join data.ethnicity e on coalesce(tmp."Ethnicity",'') = coalesce(e."name",'')
left join data.endpoint_group eg on tmp."Endpoint Group" = eg."name" 
left join data.endpoint e2 on tmp."Endpoint" = e2."name" and eg.id = e2.endpoint_group_id
left join data.incidence_entry ie on ie."year" = tmp."Year" and ie.end_age = tmp."EndAge" and ie.endpoint_group_id = eg.id and ie.endpoint_id = e2.id 
and ie.ethnicity_id = e.id and ie.gender_id = g.id and ie.incidence_dataset_id = 7 and ie.prevalence = case when lower(tmp."Type")='incidence' then false else true end 
and ie.race_id = r.id and ie.start_age = tmp."StartAge" 
where ie.id is null;


insert into data.incidence_value (grid_cell_id,grid_col , grid_row, incidence_entry_id,value)
select 
(( tmp."Column"::bigint +tmp."Row" ::bigint)*(tmp."Column"::bigint +tmp."Row" ::bigint +1)*0.5)+tmp."Row" ::bigint as grid_cell_id
,tmp."Column" as grid_col, tmp."Row" as grid_row, ie.id as incidence_entry_id, tmp."Value" as value
from data.tmp_otherIncidence_dementia tmp
left join data.race r on coalesce(tmp."Race",'') =coalesce(r."name",'')
left join data.gender g on coalesce(tmp."Gender",'') = coalesce(g."name",'')
left join data.ethnicity e on coalesce(tmp."Ethnicity",'') = coalesce(e."name",'')
left join data.endpoint_group eg on tmp."Endpoint Group" = eg."name" 
left join data.endpoint e2 on tmp."Endpoint" = e2."name" and eg.id = e2.endpoint_group_id
left join data.incidence_entry ie 
on r.id=ie.race_id and g.id=ie.gender_id and e.id = ie.ethnicity_id and e2.id=ie.endpoint_id and eg.id=ie.endpoint_group_id 
and tmp."StartAge" = ie.start_age and tmp."EndAge" = ie.end_age and tmp."Year" =ie."year"
and ie.incidence_dataset_id = 3;

drop table data.tmp_table;
drop table data.tmp_dem_hif;
drop table data.tmp_dem_vf;
drop table data.tmp_otherIncidence_dementia;
--# END OF SCRIPT