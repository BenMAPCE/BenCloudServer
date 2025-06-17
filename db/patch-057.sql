/**** Replicate valuation functions that we want to default to multiple endpoints****/

UPDATE "data".settings SET value_int=57 WHERE "key"='version';

delete from data.valuation_function where id in (591,592,593,594,595);

INSERT INTO data.valuation_function (id,valuation_dataset_id,endpoint_group_id,endpoint_id,qualifier,reference,start_age,end_age,function_text,val_a,name_a,dist_a,p1a,p2a,val_b,name_b,val_c,name_c,val_d,name_d,epa_standard) VALUES
	 (591,8,10,95,'COI: med costs + wage loss','HCUP National Inpatient Sample (NIS). Healthcare Cost and Utilization Project (HCUP). 2016. Agency for Healthcare Research and Quality, Rockville, MD. www.hcup-us.ahrq.gov/nisoverview.jsp ',0,18,'A*MedicalCostIndex+B*((average_cost_yearly)/(52*5))*WageIndex',9075.28239,'mean hospital cost, in 2015$','Normal',404.4594246,0.0,3.495217966,'mean length of stay (LOS)',0.0,'0',0.0,'0',true),
	 (592,8,12,56,'VSL, based on 26 value-of-life studies, no D.R. applied','0',0,99,'A*AllGoodsIndex',8705114.255,'mean VSL in 2015$','Weibull',9648168.299,1.509588003,0.0,'0',0.0,'0',0.0,'0',false),
	 (593,8,12,56,'VSL, based on 26 value-of-life studies, with Cessation Lag, 2% d.r.','0',0,99,'A*AllGoodsIndex*B',8705114.255,'mean VSL in 2015$','Weibull',9648168.299,1.509588003,0.93424,'2% d.r. Cessation Lag',0.0,'0',0.0,'0',true),
	 (594,8,12,56,'VSL, based on 26 value-of-life studies, with Cessation Lag, 3% d.r.','0',0,99,'A*AllGoodsIndex*B',8705114.255,'mean VSL in 2015$','Weibull',9648168.299,1.509588003,0.90605998,'3% d.r. Cessation Lag',0.0,'0',0.0,'0',true),
	 (595,8,12,56,'VSL, based on 26 value-of-life studies, with Cessation Lag, 7% d.r.','0',0,99,'A*AllGoodsIndex*B',8705114.255,'mean VSL in 2015$','Weibull',9648168.299,1.509588003,0.81604653,'7% d.r. Cessation Lag',0.0,'0',0.0,'0',true);

SELECT SETVAL('data.valuation_function_id_seq', COALESCE(MAX(id), 1) ) FROM data.valuation_function;


--correct some epa_stand_assignments

UPDATE data.valuation_function SET epa_standard = false WHERE id = 558;

VACUUM (VERBOSE, ANALYZE) "data".valuation_function;

