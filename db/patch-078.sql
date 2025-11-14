
UPDATE data.settings SET value_int=78 WHERE "key"='version';

INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
select 1 as health_impact_function_group_id, hif.id as health_impact_function_id
from data.health_impact_function hif
where hif.health_impact_function_dataset_id = 15
and hif.archived = 0
and hif.endpoint_group_id =12 --please double check
and hif.id not in (select health_impact_function_id from  data.health_impact_function_group_member);

INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
select 5 as health_impact_function_group_id,
       hif.id as health_impact_function_id
from data.health_impact_function hif
where hif.health_impact_function_dataset_id = 15
and hif.archived = 0
and (
    (hif.author = 'Di et al.' and hif.qualifier = 'Table 2 Main Analysis, Cox PH with GEE')
    or (hif.author = 'Chen et al.')
    or (hif.author = 'Pope et al.' and hif.qualifier = 'Main analysis, complex CPH model with subcohort (BMI and smoking status available); HR from Table 2')
    or (hif.author = 'Turner et al.' and (hif.qualifier = 'Table E10 HBM PM2.5, MP model, controlling for HBM O3 1982-2004 ' or hif.qualifier ='Apr-Sept. Long-term ozone exposure from averaged daily ozone data. Diseases of the respiratory system (cause of death), HBM O3 (multipollutant model data, fully adjusted HR), in relation to each 10-unit increase in ozone concentration (Table E9)'))
    or (hif.author = 'Woodruff et al.')
    or (hif.author = 'Wu et al.')
    or (hif.author = 'Turner et al.' and hif.qualifier = 'Apr-Sept. Long-term ozone exposure from averaged daily ozone data. Diseases of the respiratory system (cause of death), HBM O3 (multipollutant model data, fully adjusted HR), in relation to each 10-unit increase in ozone concentration (Table E9)')
    or (hif.author = 'Katsouyanni et al.' and hif.qualifier = 'Warm Season (April - September). Distributed Lags; Penalized splines; O3 Results (Table 24)' and hif.reference = 'Katsouyanni, K., Samet, J.M., Anderson, H.R., Atkinson, R., Le Tertre, A., Medina, S., Samoli, E., Touloumi, G., Burnett, R.T., Krewski, D., Ramsay, T., Dominici, F., Peng, R.D., Schwartz, J., Zanobetti, A. 2009. Air pollution and health: A European and North American approach (APHENA). Health Effects Institute, 5-90.')
    or (hif.author = 'Zanobetti and Schwartz' and hif.qualifier = 'Warm season (June - August). Respiratory mortality, sum of lags 0-3 (Table 1)')
);

INSERT INTO "data".health_impact_function_group_member (health_impact_function_group_id,health_impact_function_id)
select hifg.id as health_impact_function_group_id,
       hif.id as health_impact_function_id
from data.health_impact_function hif
cross join data.health_impact_function_group hifg
where hif.health_impact_function_dataset_id = 15
and hif.archived = 0
and hifg.name = 'Testing Health Impact Functions'
and (
    hif.author = 'Di et al.'
and (hif.qualifier = 'Table 2 Main Analysis, Cox PH with GEE' or hif.qualifier ='Sensitivity Analysis: Total study population stratified by race (White, Black, Asian, Hispanic, Native American), Supplemental Table S3 value from Generalized Estimating Equation (GEE) using total study sample')
);
 
