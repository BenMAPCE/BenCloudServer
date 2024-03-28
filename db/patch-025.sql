-- 3/26/2024
-- Correct annual metric in existing AQ surfaces
-- Update valuation function defaults and misaligned age ranges

UPDATE "data".settings SET value_int=25 where "key"='version';

UPDATE data.air_quality_layer_metrics set annual_statistic_id = 1;

UPDATE data.valuation_function SET epa_standard=false;
UPDATE data.valuation_function SET start_age=65 WHERE id=487;
UPDATE data.valuation_function SET start_age=65 WHERE id=492;
UPDATE data.valuation_function SET epa_standard=true WHERE id=493;
UPDATE data.valuation_function SET epa_standard=true WHERE id=494;
UPDATE data.valuation_function SET epa_standard=true WHERE id=496;
UPDATE data.valuation_function SET epa_standard=true WHERE id=498;
UPDATE data.valuation_function SET epa_standard=true WHERE id=500;
UPDATE data.valuation_function SET epa_standard=true WHERE id=502;
UPDATE data.valuation_function SET epa_standard=true WHERE id=503;
UPDATE data.valuation_function SET epa_standard=true WHERE id=504;
UPDATE data.valuation_function SET epa_standard=true WHERE id=505;
UPDATE data.valuation_function SET epa_standard=true WHERE id=506;
UPDATE data.valuation_function SET epa_standard=true WHERE id=507;
UPDATE data.valuation_function SET epa_standard=true WHERE id=508;
UPDATE data.valuation_function SET epa_standard=true WHERE id=509;
UPDATE data.valuation_function SET epa_standard=true WHERE id=513;
UPDATE data.valuation_function SET epa_standard=true WHERE id=514;
UPDATE data.valuation_function SET epa_standard=true WHERE id=517;
UPDATE data.valuation_function SET epa_standard=true WHERE id=518;
UPDATE data.valuation_function SET epa_standard=true WHERE id=519;
UPDATE data.valuation_function SET epa_standard=true WHERE id=522;
UPDATE data.valuation_function SET epa_standard=true WHERE id=541;
UPDATE data.valuation_function SET epa_standard=true WHERE id=547;
UPDATE data.valuation_function SET epa_standard=true WHERE id=550;
UPDATE data.valuation_function SET epa_standard=true WHERE id=551;
UPDATE data.valuation_function SET epa_standard=true WHERE id=552;
UPDATE data.valuation_function SET epa_standard=true WHERE id=553;
UPDATE data.valuation_function SET epa_standard=true WHERE id=554;
UPDATE data.valuation_function SET epa_standard=true WHERE id=555;
UPDATE data.valuation_function SET epa_standard=true WHERE id=556;
UPDATE data.valuation_function SET epa_standard=true WHERE id=557;
UPDATE data.valuation_function SET epa_standard=true WHERE id=558;
UPDATE data.valuation_function SET epa_standard=true WHERE id=559;
UPDATE data.valuation_function SET epa_standard=true WHERE id=560;
UPDATE data.valuation_function SET epa_standard=true WHERE id=561;
UPDATE data.valuation_function SET epa_standard=true WHERE id=562;
UPDATE data.valuation_function SET epa_standard=true WHERE id=563;
UPDATE data.valuation_function SET epa_standard=true, start_age=65 WHERE id=564;
UPDATE data.valuation_function SET epa_standard=true WHERE id=565;
UPDATE data.valuation_function SET epa_standard=true WHERE id=566;