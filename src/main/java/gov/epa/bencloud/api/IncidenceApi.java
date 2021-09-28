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

	public static boolean addIncidenceEntryGroups(HIFTaskConfig hifTaskConfig, HIFConfig hifConfig, HealthImpactFunctionRecord hifRecord, ArrayList<Map<Integer, Map<Integer, Double>>> incidenceLists, Map<String, Integer> incidenceCacheMap) {

		Map<Integer, Map<Integer, Double>> incidenceMap = new HashMap<Integer, Map<Integer, Double>>();
		
		//Some functions don't use incidence or prevalence. Just return an empty map for those.
		if((hifConfig.incidence == null || hifConfig.incidence == 0) && (hifConfig.prevalence == null || hifConfig.prevalence == 0)) {
			incidenceLists.add(incidenceMap);
			return true;
		}
		
		//Build a unique cache key for this incidence/prevalence result set
		Integer incPrevId = hifConfig.prevalence == null || hifConfig.prevalence == 0 ? hifConfig.incidence : hifConfig.prevalence;
		Integer incPrevYear = hifConfig.prevalence == null || hifConfig.prevalence == 0 ? hifConfig.incidenceYear : hifConfig.prevalenceYear;
		
		// Now, check the incidenceLists to see if we already have data for this function config
		String cacheKey = incPrevId + "~" + incPrevYear + "~" + hifRecord.getEndpointId() + "~" + hifConfig.startAge + "~" + hifConfig.endAge;
		
		if(incidenceCacheMap.containsKey(cacheKey)) {
			// Just add another reference to this map in the incidenceLists ArrayList
			incidenceLists.add(incidenceLists.get(incidenceCacheMap.get(cacheKey)));
			return true;
		}
		
		// We don't already have results for this type of config, so keep track in this lookup map in case another function needs it
		incidenceCacheMap.put(cacheKey, incidenceLists.size());
		
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
			HashMap<Integer, Double> incidenceCellMap = new HashMap<Integer, Double>();

			// FOR EACH POPULATION AGE RANGE
			for (Record3<Integer, Short, Short> popAgeRange : popAgeRanges) {
				
				// FOR EACH INCIDENCE AGE RANGE
				int count=0;
				for (GetIncidenceRecord incidenceAgeRange : cellIncidence.getValue()) {
					Short popAgeStart = popAgeRange.value2();
					Short popAgeEnd = popAgeRange.value3();
					Short incAgeStart = incidenceAgeRange.getStartAge();
					Short incAgeEnd = incidenceAgeRange.getEndAge();
					//If the full population age range fits within the incidence age range, then apply this incidence rate to the population age range
					if (popAgeStart >= incAgeStart && popAgeEnd <= incAgeEnd) {
						incidenceCellMap.put(popAgeRange.value1(), incidenceCellMap.getOrDefault(popAgeRange.value1(), 0.0) + incidenceAgeRange.getValue().doubleValue());
						count++;
					} //TODO: add more intricate checks here to handle overlap with appropriate weighting
					
				}
				//Now calculate the average (if more than one incidence rate was applied to this population age range
				if(count > 0) {
					incidenceCellMap.put(popAgeRange.value1(), incidenceCellMap.getOrDefault(popAgeRange.value1(), 0.0)/count);
				}
			}
			incidenceMap.put(cellIncidence.getKey(), incidenceCellMap);
		}
		incidenceLists.add(incidenceMap);
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
