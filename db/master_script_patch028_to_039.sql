/********************************************************************************************************************************/
/*************************************************Database updates from patch028 to patch034.************************************/
/********************************************************************************************************************************/
--Run this script using psql using user benmap_system.
--The benmap_system user MUST have full access to the database, including the ability to modify, create, and delete tables.
--Please ensure all *.sql files are placed in the same folder.
--After execution, use generate_sequence_reset_script.sql to generate scripts to reset sequential fields.


\i patch-028.sql

--Add population growth  
\i population_growth.sql
\i population_growth_weight.sql
\i t_pop_dataset_year.sql

\i patch-029.sql
\i patch-030.sql
\i patch-031.sql
\i patch-032.sql
\i patch-033.sql
\i patch-034.sql
\i patch-035.sql
\i patch-036.sql
\i patch-037.sql

--2020 datasets. 
\i grid_definition_2020.sql
\i population_dataset_2020.sql
\i population_entry_2020.sql
\i population_entry_2020_tract.sql
\i population_value_2020.sql
\i population_value_2020_tract.sql
\i population_growth_2020_1_partition.sql
\i population_growth_2020_2_data.sql
\i population_growth_weight_2020.sql
\i population_growth_weight_2020_tract.sql
\i crosswalk_dataset_2020.sql
\i crosswalk_entry_2020.sql
\i incidence_dataset_2020.sql
\i incidence_entry_2020.sql
\i incidence_value_2020.sql
\i variable_dataset_2020.sql
\i variable_entry_2020.sql
\i variable_value_2020.sql
\i post_adding_2020_data.sql
\i population_dataset_2020_County.sql
\i population_entry_2020_County.sql
\i population_value_2020_County.sql
\i t_pop_dataset_year_2020_County.sql
\i population_growth_weight_2020_County.sql
\i post_adding_2020_County_data.sql

\i patch-038.sql
\i patch-039.sql

-- EOF
