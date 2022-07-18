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
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetIncidenceRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import gov.epa.bencloud.server.tasks.local.HIFTaskRunnable;
import spark.Request;
import spark.Response;

public class IncidenceApi {

	public static boolean addIncidenceOrPrevalenceEntryGroups(HIFTaskConfig hifTaskConfig, HIFConfig hifConfig, boolean isIncidence, Record h, ArrayList<Map<Long, Map<PopulationApi.DemographicGroup, Double>>> incidenceOrPrevalenceLists, Map<String, Integer> incidenceOrPrevalenceCacheMap) {

		//isIncidence tells us whether we should be loading incidence or prevalence
		
		//TODO: YY: incidenceOrPrevalenceMapOld is how it was originally calculated using ageRangeId as the key. Can be removed after confirming the new method is working.
		
		Map<Long, Map<Integer, Double>> incidenceOrPrevalenceMapOld = new HashMap<Long, Map<Integer, Double>>();
		Map<Long, Map<PopulationApi.DemographicGroup, Double>> incidenceOrPrevalenceMap = new HashMap<Long, Map<PopulationApi.DemographicGroup, Double>>();
		
		//Some functions don't use incidence or prevalence. Just return an empty map for those.
		if(isIncidence==true && (hifConfig.incidence == null || hifConfig.incidence == 0)) {
			//incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMapOld);
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		} else if (isIncidence==false & (hifConfig.prevalence == null || hifConfig.prevalence == 0)) {
			//incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMapOld);
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		}
		
		//Build a unique cache key for this incidence/prevalence result set
		Integer incPrevId = isIncidence ? hifConfig.incidence : hifConfig.prevalence;
		Integer incPrevYear = isIncidence ? hifConfig.incidenceYear : hifConfig.prevalenceYear;
		
		// Now, check the incidenceOrPrevalenceLists to see if we already have data for this function config
		String cacheKey = incPrevId + "~" + incPrevYear + "~" + h.get("endpoint_id", Integer.class) + "~" + hifConfig.startAge + "~" + hifConfig.endAge;
		
		if(incidenceOrPrevalenceCacheMap.containsKey(cacheKey)) {
			// Just add another reference to this map in the incidenceLists ArrayList
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceLists.get(incidenceOrPrevalenceCacheMap.get(cacheKey)));
			return true;
		}
		
		// We don't already have results for this type of config, so keep track in this lookup map in case another function needs it
		incidenceOrPrevalenceCacheMap.put(cacheKey, incidenceOrPrevalenceLists.size());
		
		//Return an average incidence for each population age range for a given hif
		//TODO: Need to add in handling for race, ethnicity, gender. 
		// Right now, when we're using National Incidence/Prevalence, getIncidence is averaging, otherwise it's summing. This is to match desktop, but needs to be revised.
		
		//YY: age range percentage?
		ArrayList<HashMap<Integer, Double>> hifPopAgeRangeMapping = HIFTaskRunnable.getPopAgeRangeMapping(hifTaskConfig);
		
		//Get array of race, ethnicity and gender to include based on the configured hifs
        //TODO: If all hifs calls for "all" or null, set groupby = false. Will the values in lookup table stay forever? 
        ArrayList<Integer> raceIds = PopulationApi.getRacesForHifs(hifTaskConfig);
        Integer arrRaceIds[] = new Integer[raceIds.size()];
        arrRaceIds = raceIds.toArray(arrRaceIds);
        boolean booGroupByRace = true;  //1ASIAN, 2BLACK, 3NATAMER, 4WHITE, 5All, 6null     
        
        ArrayList<Integer> ethnicityIds = PopulationApi.getEthnicityForHifs(hifTaskConfig);
        Integer arrEthnicityIds[] = new Integer[ethnicityIds.size()];
        arrEthnicityIds = ethnicityIds.toArray(arrEthnicityIds);
        boolean booGroupByEthnicity = true;  //1NON-HISP, 2HISP, 3All, 4null       
        
        ArrayList<Integer> genderIds = PopulationApi.getGendersForHifs(hifTaskConfig);
        Integer arrGenderIds[] = new Integer[genderIds.size()];
        arrGenderIds = genderIds.toArray(arrGenderIds);
        boolean booGroupByGender = true; //1F, 2M, 3All, 4null 
		
		Map<Long, Result<GetIncidenceRecord>> incRecords = Routines.getIncidence(JooqUtil.getJooqConfiguration(), 
				incPrevId,
				incPrevYear,
				h.get("endpoint_id", Integer.class), 
				arrRaceIds, 
				arrEthnicityIds, 
				arrGenderIds, 
				hifConfig.startAge.shortValue(), 
				hifConfig.endAge.shortValue(), 
				booGroupByRace,
				booGroupByEthnicity, 
				booGroupByGender,
				true, 
				AirQualityApi.getAirQualityLayerGridId(hifTaskConfig.aqBaselineId))
				.intoGroups(GET_INCIDENCE.GRID_CELL_ID);
		
		
		// Get the age groups for the population dataset
		Result<Record3<Integer, Short, Short>> popAgeRanges = PopulationApi.getPopAgeRanges(hifTaskConfig.popId);

		// Build a nested map like <grid_cell_id, <age_group_id, incidence_value>>
		// YY: change age_group_id key to an object which contains age_group_id, race_id, ethnicity_id, and gender_id
		
		// FOR EACH GRID CELL
		for (Entry<Long, Result<GetIncidenceRecord>> cellIncidence : incRecords.entrySet()) {
			HashMap<Integer, Double> incidenceOrPrevalenceCellMap = new HashMap<Integer, Double>();
			HashMap<PopulationApi.DemographicGroup, Double> incidenceOrPrevalenceCellMap2 = new HashMap<PopulationApi.DemographicGroup, Double>();
			
			

			// FOR EACH POPULATION AGE RANGE
			for (Record3<Integer, Short, Short> popAgeRange : popAgeRanges) {
				
				// FOR EACH INCIDENCE AGE RANGE
				int count=0;
				HashMap<PopulationApi.DemographicGroup, Integer> demoGroupCount = new HashMap<PopulationApi.DemographicGroup, Integer>(); //for calculating average later
				
				for (GetIncidenceRecord incidenceOrPrevalenceAgeRange : cellIncidence.getValue()) {
					Short popAgeStart = popAgeRange.value2();
					Short popAgeEnd = popAgeRange.value3();
					Short incAgeStart = incidenceOrPrevalenceAgeRange.getStartAge();
					Short incAgeEnd = incidenceOrPrevalenceAgeRange.getEndAge();

					if (popAgeStart <= incAgeEnd && popAgeEnd >= incAgeStart) {
						incidenceOrPrevalenceCellMap.put(popAgeRange.value1(), incidenceOrPrevalenceCellMap.getOrDefault(popAgeRange.value1(), 0.0) + incidenceOrPrevalenceAgeRange.getValue().doubleValue());
						
						count++;
					} 
					
					//YY: Correct?
					PopulationApi.DemographicGroup demoGroup = new PopulationApi.DemographicGroup();
					demoGroup.ageRangeId = popAgeRange.value1();
					demoGroup.raceId = incidenceOrPrevalenceAgeRange.getRaceId();
					demoGroup.ethnicityId = incidenceOrPrevalenceAgeRange.getEthnicityId();
					demoGroup.genderId = incidenceOrPrevalenceAgeRange.getGenderId();
					
					HashMap<Integer, Double> popAgeRangeHifMap = hifPopAgeRangeMapping.get(hifConfig.arrayIdx);
					if (popAgeRangeHifMap.containsKey(demoGroup.ageRangeId)) {
						double inc = incidenceOrPrevalenceCellMap2.getOrDefault(demoGroup, 0.0) * popAgeRangeHifMap.get(demoGroup.ageRangeId);
						incidenceOrPrevalenceCellMap2.put(demoGroup, incidenceOrPrevalenceCellMap2.getOrDefault(demoGroup, 0.0) + inc);
						demoGroupCount.put(demoGroup, demoGroupCount.getOrDefault(demoGroup, 0) + 1);
					}	
					
				}
				//Now calculate the average (if more than one incidence rate was applied to this population age range
				//TODO: Do we need to improve our averaging here to handle partial overlaps better
				
				if(count > 0) {
					incidenceOrPrevalenceCellMap.put(popAgeRange.value1(), incidenceOrPrevalenceCellMap.getOrDefault(popAgeRange.value1(), 0.0)/count);					
				}
				
				//YY: averaging by demographic groups (age, race, ethnicity, gender)
				if(!demoGroupCount.isEmpty()) {
					for(Entry<PopulationApi.DemographicGroup, Integer> entry : demoGroupCount.entrySet()) {
						PopulationApi.DemographicGroup demoGroup = entry.getKey();
						int groupCount = entry.getValue();
						incidenceOrPrevalenceCellMap2.put(demoGroup, incidenceOrPrevalenceCellMap2.getOrDefault(demoGroup, 0.0)/groupCount); 						
					}
					
					//
				}
				
			}
			incidenceOrPrevalenceMapOld.put(cellIncidence.getKey(), incidenceOrPrevalenceCellMap);
			incidenceOrPrevalenceMap.put(cellIncidence.getKey(), incidenceOrPrevalenceCellMap2);
		}
		//incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMapOld);
		incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
		return true;
	}
	
	public static Object getAllIncidenceDatasets(Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(response, false);
	}

	public static Object getAllPrevalenceDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(response, true);
	}
	
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

	public static String getIncidenceDatasetName(int id) {

		Record1<String> record = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME)
				.from(INCIDENCE_DATASET)
				.where(INCIDENCE_DATASET.ID.eq(id))
				.fetchOne();
		
		return record.value1();
	}

}
