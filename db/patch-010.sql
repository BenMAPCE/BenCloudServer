-- Fix the age range for Asthma Symtoms Albuterol Use incidence

UPDATE "data".settings SET value_int=10 where "key"='version';

UPDATE "data".incidence_entry SET start_age=6, end_age=17 WHERE id=4552;
UPDATE "data".incidence_value SET value=0.0904 WHERE incidence_entry_id=4552;

