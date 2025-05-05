/**** County, ethnicity-adjusted race-stratified (2020) was incorrectly marked as prevalence data. Correct it. ***/

UPDATE "data".settings SET value_int=51 WHERE "key"='version';

update data.incidence_entry set prevalence = false where incidence_dataset_id = 9;