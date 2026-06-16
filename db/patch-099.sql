/**** Update broken HIF access urls ****/

UPDATE data.settings SET value_int=99 WHERE "key"='version';

-- Group 5: ResearchGate PDF -> PMC (Zanobetti & Schwartz 2006, J Epidemiol Community Health)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2566060/'
  WHERE id IN (946, 947, 948, 949, 950, 1039);

-- Group 6: BioMedCentral -> PMC (Zanobetti et al. 2009, Environ Health)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2807856/'
  WHERE id IN (951, 952, 953, 954, 955, 980, 1040);

-- Groups 7 & 45: ScienceDirect wrong PII -> correct PII (Ransom & Pope 1992, Environ Res)
UPDATE data.health_impact_function SET access_url = 'https://www.sciencedirect.com/science/article/abs/pii/S0013935105802166'
  WHERE id IN (862, 886, 960, 961);

-- Group 9: atsjournals.org -> Oxford Academic (Rabinovitch et al. 2006, AJRCCM)
UPDATE data.health_impact_function SET access_url = 'https://academic.oup.com/ajrccm/article/173/10/1098/8528223'
  WHERE id = 967;

-- Group 11: Expired CloudFront URL -> Nature/JESEE (Alhanti et al. 2016)
UPDATE data.health_impact_function SET access_url = 'https://www.nature.com/articles/jes201557'
  WHERE id IN (1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010);

-- Group 14: BioMedCentral -> PMC (Villeneuve et al. 2007, Environ Health)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2254596/'
  WHERE id IN (907, 908, 909, 910, 911, 912);

-- Group 16: ehp.niehs.nih.gov -> PMC (Krall et al. 2017, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC5226704/'
  WHERE id IN (970, 971, 972, 973);

-- Group 19: ehp.niehs.nih.gov -> PMC (Kioumourtzoglou et al. 2016, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC4710596/'
  WHERE id IN (959, 975);

-- Group 22: ehp.niehs.nih.gov -> PMC (Ostro et al. 2009, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2661920/'
  WHERE id = 958;

-- Group 23: ResearchGate PDF -> AAP Pediatrics (Lin/Stieb/Chen 2005, Pediatrics)
UPDATE data.health_impact_function SET access_url = 'https://publications.aap.org/pediatrics/article/116/2/e235/62909/Coarse-Particulate-Matter-and-Hospitalization-for'
  WHERE id IN (869, 870, 917, 918);

-- Group 26: ehp.niehs.nih.gov -> PMC (Rosenthal et al. 2008, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2367645/'
  WHERE id = 963;

-- Group 30: ehp.niehs.nih.gov -> PMC (McConnell et al. 2010, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2920902/'
  WHERE id = 986;

-- Group 31: atsjournals.org -> Oxford Academic (Nishimura et al. 2013, AJRCCM)
UPDATE data.health_impact_function SET access_url = 'https://academic.oup.com/ajrccm/article/188/3/309/8506872'
  WHERE id = 987;

-- Group 32: ehp.niehs.nih.gov -> PMC (Tetreault et al. 2016, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC4977042/'
  WHERE id IN (888, 889, 968, 969);

-- Group 33: ehp.niehs.nih.gov -> PMC (Parker et al. 2009, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2627858/'
  WHERE id IN (871, 887, 976);

-- Group 34: ehp.niehs.nih.gov -> PMC (Gharibvand et al. 2017, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC5332173/'
  WHERE id IN (965, 1045, 1046, 1047, 1048, 1049, 1050, 1051);

-- Group 37: ehp.niehs.nih.gov -> PMC (Pope et al. 2019, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC6792459/'
  WHERE id IN (1012, 1013, 1014, 1015, 1016, 1017);

-- Group 38: atsjournals.org -> Oxford Academic (Turner et al. 2016, AJRCCM)
UPDATE data.health_impact_function SET access_url = 'https://academic.oup.com/ajrccm/article/193/10/1134/8508927'
  WHERE id IN (896, 978, 993, 890, 897, 898);

-- Group 39: ehp.niehs.nih.gov -> PMC (Woodruff et al. 2008, EHP)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2199284/'
  WHERE id = 977;

-- Group 41: BioMedCentral -> PMC (Zanobetti & Schwartz 2008, Environ Health - ozone mortality)
UPDATE data.health_impact_function SET access_url = 'https://pmc.ncbi.nlm.nih.gov/articles/PMC2429903/'
  WHERE id IN (921, 922, 923, 924, 925, 926, 927, 928);

-- Group 46: atsjournals.org -> Oxford Academic (Zanobetti & Schwartz 2008, AJRCCM)
UPDATE data.health_impact_function SET access_url = 'https://academic.oup.com/ajrccm/article/177/2/184/8517942'
  WHERE id = 891;
