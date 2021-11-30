package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jooq.JSONFormat;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record1;
import org.jooq.impl.DSL;

import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetIncidenceRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import spark.Response;

public class IncidenceApi {

	public static boolean addIncidenceOrPrevalenceEntryGroups(HIFTaskConfig hifTaskConfig, HIFConfig hifConfig, boolean isIncidence, HealthImpactFunctionRecord hifRecord, ArrayList<Map<Integer, Map<Integer, Double>>> incidenceOrPrevalenceLists, Map<String, Integer> incidenceOrPrevalenceCacheMap) {

		//isIncidence tells us whether we should be loading incidence or prevalence
		
		Map<Integer, Map<Integer, Double>> incidenceOrPrevalenceMap = new HashMap<Integer, Map<Integer, Double>>();
		
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
		
		// Now, check the incidenceOrPrevalenceLists to see if we already have data for this function config
		String cacheKey = incPrevId + "~" + incPrevYear + "~" + hifRecord.getEndpointId() + "~" + hifConfig.startAge + "~" + hifConfig.endAge;
		
		if(incidenceOrPrevalenceCacheMap.containsKey(cacheKey)) {
			// Just add another reference to this map in the incidenceLists ArrayList
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceLists.get(incidenceOrPrevalenceCacheMap.get(cacheKey)));
			return true;
		}
		
		// We don't already have results for this type of config, so keep track in this lookup map in case another function needs it
		incidenceOrPrevalenceCacheMap.put(cacheKey, incidenceOrPrevalenceLists.size());
		
		//Return an average incidence for each population age range for a given hif
		//TODO: Need to add in handling for race, ethnicity, gender
		
		Map<Integer, Result<GetIncidenceRecord>> incRecords = Routines.getIncidence(JooqUtil.getJooqConfiguration(), 
				incPrevId,
				incPrevYear,
				hifRecord.getEndpointId(), 
				null, 
				null, 
				null, 
				hifConfig.startAge.shortValue(), 
				hifConfig.endAge.shortValue(), 
				null,
				null, 
				null,
				true, 
				AirQualityApi.getAirQualityLayerGridId(hifTaskConfig.aqBaselineId)).intoGroups(GET_INCIDENCE.GRID_CELL_ID);
		
		
		// Get the age groups for the population dataset
		Result<Record3<Integer, Short, Short>> popAgeRanges = PopulationApi.getPopAgeRanges(hifTaskConfig.popId);

		// Build a nested map like <grid_cell_id, <age_group_id, incidence_value>>
		
		// FOR EACH GRID CELL
		for (Entry<Integer, Result<GetIncidenceRecord>> cellIncidence : incRecords.entrySet()) {
			HashMap<Integer, Double> incidenceOrPrevalenceCellMap = new HashMap<Integer, Double>();

			// FOR EACH POPULATION AGE RANGE
			for (Record3<Integer, Short, Short> popAgeRange : popAgeRanges) {
				
				// FOR EACH INCIDENCE AGE RANGE
				int count=0;
				for (GetIncidenceRecord incidenceOrPrevalenceAgeRange : cellIncidence.getValue()) {
					Short popAgeStart = popAgeRange.value2();
					Short popAgeEnd = popAgeRange.value3();
					Short incAgeStart = incidenceOrPrevalenceAgeRange.getStartAge();
					Short incAgeEnd = incidenceOrPrevalenceAgeRange.getEndAge();
					//If the full population age range fits within the incidence age range, then apply this incidence rate to the population age range
					if (popAgeStart >= incAgeStart && popAgeEnd <= incAgeEnd) {
						incidenceOrPrevalenceCellMap.put(popAgeRange.value1(), incidenceOrPrevalenceCellMap.getOrDefault(popAgeRange.value1(), 0.0) + incidenceOrPrevalenceAgeRange.getValue().doubleValue());
						count++;
					} //TODO: add more intricate checks here to handle overlap with appropriate weighting
					
				}
				//Now calculate the average (if more than one incidence rate was applied to this population age range
				if(count > 0) {
					incidenceOrPrevalenceCellMap.put(popAgeRange.value1(), incidenceOrPrevalenceCellMap.getOrDefault(popAgeRange.value1(), 0.0)/count);
				}
			}
			incidenceOrPrevalenceMap.put(cellIncidence.getKey(), incidenceOrPrevalenceCellMap);
		}
		incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
		return true;
	}
	
	public static Object getAllIncidenceDatasets(Response response) {
		return getAllIncidencePrevalenceDatasets(response, false);
	}

	public static Object getAllPrevalenceDatasets(Response response) {
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
