-- Update EPA standard valuation functions. Enable 3% and 7% DR in addition to 2%

UPDATE "data".settings SET value_int=42 WHERE "key"='version';

UPDATE data.valuation_function SET epa_standard=true WHERE id=483;
UPDATE data.valuation_function SET epa_standard=true WHERE id=484;
UPDATE data.valuation_function SET epa_standard=true WHERE id=485;
UPDATE data.valuation_function SET epa_standard=true WHERE id=486;
UPDATE data.valuation_function SET epa_standard=true WHERE id=487;
UPDATE data.valuation_function SET epa_standard=true WHERE id=488;
UPDATE data.valuation_function SET epa_standard=true WHERE id=489;
UPDATE data.valuation_function SET epa_standard=true WHERE id=490;
UPDATE data.valuation_function SET epa_standard=true WHERE id=491;
UPDATE data.valuation_function SET epa_standard=true WHERE id=492;
UPDATE data.valuation_function SET epa_standard=true WHERE id=520;
UPDATE data.valuation_function SET epa_standard=true WHERE id=521;
UPDATE data.valuation_function SET epa_standard=true WHERE id=523;
UPDATE data.valuation_function SET epa_standard=true WHERE id=524;
UPDATE data.valuation_function SET epa_standard=true WHERE id=525;
UPDATE data.valuation_function SET epa_standard=true WHERE id=526;
UPDATE data.valuation_function SET epa_standard=true WHERE id=527;
UPDATE data.valuation_function SET epa_standard=true WHERE id=528;
UPDATE data.valuation_function SET epa_standard=true WHERE id=529;
UPDATE data.valuation_function SET epa_standard=true WHERE id=530;
UPDATE data.valuation_function SET epa_standard=true WHERE id=531;
UPDATE data.valuation_function SET epa_standard=true WHERE id=532;
UPDATE data.valuation_function SET epa_standard=true WHERE id=533;
UPDATE data.valuation_function SET epa_standard=true WHERE id=534;
UPDATE data.valuation_function SET epa_standard=true WHERE id=535;
UPDATE data.valuation_function SET epa_standard=true WHERE id=536;
UPDATE data.valuation_function SET epa_standard=true WHERE id=537;
UPDATE data.valuation_function SET epa_standard=true WHERE id=538;
UPDATE data.valuation_function SET epa_standard=true WHERE id=539;
UPDATE data.valuation_function SET epa_standard=true WHERE id=540;
UPDATE data.valuation_function SET epa_standard=true WHERE id=542;
UPDATE data.valuation_function SET epa_standard=true WHERE id=543;
UPDATE data.valuation_function SET epa_standard=true WHERE id=545;
UPDATE data.valuation_function SET epa_standard=true WHERE id=546;