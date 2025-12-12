/*BWD 255 Update valuation function url and reference for Motality long-term, short-term, and infant functions. */

UPDATE "data".settings SET value_int=87 where "key"='version';

update data.valuation_function 
set reference = 'Viscusi, K. (1992). Fatal Tradeoffs: Public and Private Responsibilities for Risk. Oxford University Press'
, access_url = 'https://global.oup.com/academic/product/fatal-tradeoffs-9780195102932?cc=us&lang=en&'
where endpoint_id in (
	select e.id from data.endpoint e 
	inner join data.endpoint_group eg on e.endpoint_group_id  = eg.id
	where eg.user_id is null 
	and e.display_name in ('Mortality, All Cause','Mortality, Respiratory')
)
and archived = 0;