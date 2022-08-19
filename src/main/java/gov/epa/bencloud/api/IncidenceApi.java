package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.jooq.JSONFormat;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;

import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetIncidenceRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.tasks.local.HIFTaskRunnable;
import spark.Request;
import spark.Response;

public class IncidenceApi {

	/**
	 * @param hifTaskConfig
	 * @param hifConfig
	 * @param isIncidence if true, load incidence; if false, load prevelance
	 * @param h
	 * @param incidenceOrPrevalenceLists
	 * @param incidenceOrPrevalenceCacheMap
	 * @return true if incidence or prevelance entry groups are successfully added to incidenceOrPrevelanceLists,
	 * 			or if incidence/prevalence are not used to for the given function
	 */
	public static boolean addIncidenceOrPrevalenceEntryGroups(HIFTaskConfig hifTaskConfig, ArrayList<HashMap<Integer, Double>> hifPopAgeRangeMapping, HIFConfig hifConfig, boolean isIncidence, Record h, ArrayList<Map<Long, Map<PopulationCategoryKey, Double>>> incidenceOrPrevalenceLists, Map<String, Integer> incidenceOrPrevalenceCacheMap) {

		//isIncidence tells us whether we should be loading incidence or prevalence
		
		Map<Long, Map<Integer, Double>> incidenceOrPrevalenceMapOld = new HashMap<Long, Map<Integer, Double>>();
		Map<Long, Map<PopulationCategoryKey, Double>> incidenceOrPrevalenceMap = new HashMap<Long, Map<PopulationCategoryKey, Double>>();
		
		//Some functions don't use incidence or prevalence. Just return an empty map for those.
		if(isIncidence==true && (hifConfig.incidence == null || hifConfig.incidence == 0)) {
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		} else if (isIncidence==false & (hifConfig.prevalence == null || hifConfig.prevalence == 0)) {
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		}
		
		//Build a unique cache key for this incidence/prevalence result set
		Integer incPrevId = isIncidence ? hifConfig.incidence : hifConfig.prevalence;
		Integer incPrevYear = isIncidence ? hifConfig.incidenceYear : hifConfig.prevalenceYear;
		
		//Use the race, ethnicity, and gender selected earlier in order to look up the exact data this function needs
		//If needed, convert the ALL entry to the Null entry to match incidence data
		Integer raceId = (isIncidence ? hifConfig.incidenceRace : hifConfig.prevalenceRace);
		raceId = (raceId == 5 ? 6 : raceId);
		Integer ethnicityId = (isIncidence ? hifConfig.incidenceEthnicity : hifConfig.prevalenceEthnicity);
		ethnicityId = (ethnicityId == 3 ? 4 : ethnicityId);
		Integer genderId = (isIncidence ? hifConfig.incidenceGender : hifConfig.prevalenceGender);
		genderId = (genderId == 3 ? 4 : genderId);
		
		Integer hifAgeStart = hifConfig.startAge;
		Integer hifAgeEnd = hifConfig.endAge;
		
		// Now, check the incidenceOrPrevalenceLists to see if we already have data for this function config
		String cacheKey = incPrevId + "~" + incPrevYear + "~" + h.get("endpoint_id", Integer.class) + "~" + hifConfig.startAge + "~" + hifConfig.endAge
				+ "~" + raceId
				+ "~" + ethnicityId
				+ "~" + genderId;
		
		if(incidenceOrPrevalenceCacheMap.containsKey(cacheKey)) {
			// If we found it, just add another reference to this map in the incidenceLists ArrayList
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceLists.get(incidenceOrPrevalenceCacheMap.get(cacheKey)));
			return true;
		}
		
		// We don't already have results for this type of config, so keep track in this lookup map in case another function needs it
		incidenceOrPrevalenceCacheMap.put(cacheKey, incidenceOrPrevalenceLists.size());
		
		//Return an average incidence for each population age range for a given hif
		//TODO: Need to add in handling for race, ethnicity, gender. 
		// Right now, when we're using National Incidence/Prevalence, getIncidence is averaging, otherwise it's summing. This is to match desktop, but needs to be revised.
				
		//Convert race, ethnicity, gender to single element arrays
		Integer[] arrRaceId = new Integer[1];
		arrRaceId[0] = raceId;
		Integer[] arrEthnicityId = new Integer[1];
		arrEthnicityId[0] = ethnicityId;
		Integer[] arrGenderId = new Integer[1];
		arrGenderId[0] = genderId;
		
		Map<Long, Result<GetIncidenceRecord>> incRecords = Routines.getIncidence(JooqUtil.getJooqConfiguration(), 
				incPrevId,
				incPrevYear,
				h.get("endpoint_id", Integer.class), 
				arrRaceId, 
				arrEthnicityId,
				arrGenderId,
				hifConfig.startAge.shortValue(), 
				hifConfig.endAge.shortValue(), 
				false,
				false,
				false, 
				true, 
				AirQualityApi.getAirQualityLayerGridId(hifTaskConfig.aqBaselineId))
				.intoGroups(GET_INCIDENCE.GRID_CELL_ID);
		
		
		// Get the age groups for the population dataset
		Result<Record3<Integer, Short, Short>> popAgeRanges = PopulationApi.getPopAgeRanges(hifTaskConfig.popId);

		// Build a nested map like <grid_cell_id, <age_group_id, incidence_value>>
		// YY: change age_group_id key to an object which contains age_group_id, race_id, ethnicity_id, and gender_id
		
		// FOR EACH GRID CELL
		for (Entry<Long, Result<GetIncidenceRecord>> cellIncidence : incRecords.entrySet()) {
			//HashMap<Integer, Double> incidenceOrPrevalenceCellMap = new HashMap<Integer, Double>();
			HashMap<PopulationCategoryKey, Double> incidenceOrPrevalenceCellMap = new HashMap<PopulationCategoryKey, Double>();

			// FOR EACH POPULATION AGE RANGE
			for (Record3<Integer, Short, Short> popAgeRange : popAgeRanges) {
				
				// FOR EACH INCIDENCE AGE RANGE
				int count=0;
				HashMap<PopulationCategoryKey, Double> demoGroupCount = new HashMap<PopulationCategoryKey, Double>(); //for calculating average later
				for (GetIncidenceRecord incidenceOrPrevalenceAgeRange : cellIncidence.getValue()) {
					Short popAgeStart = popAgeRange.value2();
					Short popAgeEnd = popAgeRange.value3();
					Short incAgeStart = incidenceOrPrevalenceAgeRange.getStartAge();
					Short incAgeEnd = incidenceOrPrevalenceAgeRange.getEndAge();
					
					//The race,ethnicity,and gender aren't important here because the entire resultset is for the combo that the HIF needs
					PopulationCategoryKey demoGroup = new PopulationCategoryKey(popAgeRange.value1(), 
							null, //incidenceOrPrevalenceAgeRange.getRaceId(),
							null, //incidenceOrPrevalenceAgeRange.getEthnicityId(),
							null //incidenceOrPrevalenceAgeRange.getGenderId()
							);

					
					HashMap<Integer, Double> popAgeRangeHifMap = hifPopAgeRangeMapping.get(hifConfig.arrayIdx);

					//Only consider population bins that fall within the incidence range
					if (popAgeStart <= incAgeEnd && popAgeEnd >= incAgeStart) {
						
						//calculate pct for mapping inc to pop age range and hif age range. 
						double pctIncToPop = (Math.min(popAgeEnd, Math.min(incAgeEnd, hifAgeEnd)) - Math.max(popAgeStart, Math.max(incAgeStart, hifAgeStart)) + 1.0)
								/ (Math.min(popAgeEnd,hifAgeEnd) - Math.max(popAgeStart,hifAgeStart) + 1.0);
						
						if (popAgeRangeHifMap.containsKey(demoGroup.getAgeRangeId())) {
							double inc = incidenceOrPrevalenceAgeRange.getValue().doubleValue() * pctIncToPop;
							incidenceOrPrevalenceCellMap.put(demoGroup, incidenceOrPrevalenceCellMap.getOrDefault(demoGroup, 0.0) + inc);
							demoGroupCount.put(demoGroup, demoGroupCount.getOrDefault(demoGroup, 0.0) + pctIncToPop);//If we only group by age range, count per group is always 1
						}	
					}
					
				}
				
				//Weight average by age range overlap
				if(!demoGroupCount.isEmpty()) {
					for(Entry<PopulationCategoryKey, Double> entry : demoGroupCount.entrySet()) {
						PopulationCategoryKey demoGroup = entry.getKey();
						double groupCount = entry.getValue();
						incidenceOrPrevalenceCellMap.put(demoGroup, incidenceOrPrevalenceCellMap.getOrDefault(demoGroup, 0.0)/groupCount); 						
					}
					
				}
				
			}
			incidenceOrPrevalenceMap.put(cellIncidence.getKey(), incidenceOrPrevalenceCellMap);
		}
		incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
		return true;
	}
	
	/**
	 * 
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all incidence datasets.
	 */
	public static Object getAllIncidenceDatasets(Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(response, false);
	}


	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all prevalance datasets.
	 */
	public static Object getAllPrevalenceDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(response, true);
	}
	

	/**
	 * 
	 * @param response
	 * @param prevalence true if prevalance, false if incidence
	 * @return a JSON representation of all incidence or prevalance datasets.
	 */
	public static Object getAllIncidencePrevalenceDatasets(Response response, boolean prevalence) {
		Result<Record4<String, Integer, Integer, Integer[]>> records = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID,
						DSL.arrayAggDistinct(INCIDENCE_ENTRY.YEAR).orderBy(INCIDENCE_ENTRY.YEAR).as("years")
						)
				.from(INCIDENCE_DATASET)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_DATASET.ID.eq(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID))
				.where(INCIDENCE_ENTRY.PREVALENCE.eq(prevalence))
				.groupBy(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID)
				.orderBy(INCIDENCE_DATASET.NAME)
				.fetch();
		
		response.type("application/json");
		return records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}


	/**
	 * 
	 * @param id incidence dataset id
	 * @return the name of a given incidence dataset.
	 */
	public static String getIncidenceDatasetName(int id) {

		Record1<String> record = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME)
				.from(INCIDENCE_DATASET)
				.where(INCIDENCE_DATASET.ID.eq(id))
				.fetchOne();
		
		return record.value1();
	}

}
