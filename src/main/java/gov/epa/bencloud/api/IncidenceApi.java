package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.MultipartConfigElement;

import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep5;
import org.jooq.InsertValuesStep9;
import org.jooq.JSON;
import org.jooq.JSONFormat;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record13;
import org.jooq.Record16;
import org.jooq.impl.DSL;
import org.jooq.tools.csv.CSVReader;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.IncidenceUtil;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.IncidenceEntry;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetIncidenceRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceEntryRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceValueRecord;


import spark.Request;
import spark.Response;

public class IncidenceApi {
	private static final Logger log = LoggerFactory.getLogger(IncidenceApi.class);


	/**
	 * Get incidence data, categorized by population age group for the hifConfig
	 * 
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
		
		Map<Long, Map<PopulationCategoryKey, Double>> incidenceOrPrevalenceMap = new HashMap<Long, Map<PopulationCategoryKey, Double>>();
		
		//Some functions don't use incidence or prevalence. Just return an empty map for those.
		if(isIncidence==true && (hifConfig.incidence == null || hifConfig.incidence == 0)) {
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		} else if (isIncidence==false & (hifConfig.prevalence == null || hifConfig.prevalence == 0)) {
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceMap);
			return true;
		}

		Integer hifAgeStart = hifConfig.startAge;
		Integer hifAgeEnd = hifConfig.endAge;
		
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
		

		
		//Build a unique cache key for this incidence/prevalence result set
		String cacheKey = incPrevId + "~" + incPrevYear 
				+ "~" + h.get("endpoint_id", Integer.class) 
				+ "~" + hifAgeStart 
				+ "~" + hifAgeEnd
				+ "~" + raceId
				+ "~" + ethnicityId
				+ "~" + genderId;
		
		// Now, check the incidenceOrPrevalenceLists to see if we already have data for this function config
		if(incidenceOrPrevalenceCacheMap.containsKey(cacheKey)) {
			// If we found it, just add another reference to this map in the incidenceLists ArrayList
			incidenceOrPrevalenceLists.add(incidenceOrPrevalenceLists.get(incidenceOrPrevalenceCacheMap.get(cacheKey)));
			return true;
		}
		
		// We don't already have results for this type of config, so keep track in this lookup map in case another function needs it
		incidenceOrPrevalenceCacheMap.put(cacheKey, incidenceOrPrevalenceLists.size());
		
		//Return an weighted average incidence for each population age range for a given hif
		// Right now, when we're using National Incidence/Prevalence, getIncidence is averaging, otherwise it's summing. This is to match desktop, but needs to be revisited.
				
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
		
		// FOR EACH GRID CELL
		for (Entry<Long, Result<GetIncidenceRecord>> cellIncidence : incRecords.entrySet()) {
			//HashMap<Integer, Double> incidenceOrPrevalenceCellMap = new HashMap<Integer, Double>();
			HashMap<PopulationCategoryKey, Double> incidenceOrPrevalenceCellMap = new HashMap<PopulationCategoryKey, Double>();

			// FOR EACH POPULATION AGE RANGE
			for (Record3<Integer, Short, Short> popAgeRange : popAgeRanges) {
				Short popAgeStart = popAgeRange.value2();
				Short popAgeEnd = popAgeRange.value3();
				
				// FOR EACH INCIDENCE AGE RANGE
				HashMap<PopulationCategoryKey, Double> demoGroupCount = new HashMap<PopulationCategoryKey, Double>(); //for calculating average later
				for (GetIncidenceRecord incidenceOrPrevalenceAgeRange : cellIncidence.getValue()) {
					Short incAgeStart = incidenceOrPrevalenceAgeRange.getStartAge();
					Short incAgeEnd = incidenceOrPrevalenceAgeRange.getEndAge();
					
					//The race,ethnicity,and gender aren't important here because the entire resultset is for the combo that the HIF needs
					//TODO: Initially, we thought we would be needing race,ethnicity,gender in this key. 
					// Now that we know we don't, we should revert to just using popAgeRange.value1() as the key. 
					// 8/19/2022 - Leaving as-is for now to maintain stability during testing
					PopulationCategoryKey demoGroup = new PopulationCategoryKey(popAgeRange.value1(), null, null, null);

					HashMap<Integer, Double> popAgeRangeHifMap = hifPopAgeRangeMapping.get(hifConfig.arrayIdx);

					//Only consider population bins that fall within the incidence range
					if (popAgeStart <= incAgeEnd && popAgeEnd >= incAgeStart) {
						
						//calculate pct for mapping inc to intersection with pop age range and hif age range. 
						double pctIncToPop = (Math.min(popAgeEnd, Math.min(incAgeEnd, hifAgeEnd)) - Math.max(popAgeStart, Math.max(incAgeStart, hifAgeStart)) + 1.0)
								/ (Math.min(popAgeEnd,hifAgeEnd) - Math.max(popAgeStart,hifAgeStart) + 1.0);
						
						if (popAgeRangeHifMap.containsKey(demoGroup.getAgeRangeId())) {
							double inc = incidenceOrPrevalenceAgeRange.getValue().doubleValue() * pctIncToPop;
							incidenceOrPrevalenceCellMap.put(demoGroup, incidenceOrPrevalenceCellMap.getOrDefault(demoGroup, 0.0) + inc);
							demoGroupCount.put(demoGroup, demoGroupCount.getOrDefault(demoGroup, 0.0) + pctIncToPop);
						}	
					}
				}
				
				//Weighted average by age range overlap
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
	/**
	 *  @return the names of all the incidence or prevalence datasets
	 */
	public static List<String> getAllIncidencePrevalenceDatasetNames() {
    Result<Record1<String>> datasetNames = DSL.using(JooqUtil.getJooqConfiguration())
            .select(INCIDENCE_DATASET.NAME)
            .from(INCIDENCE_DATASET)
            .join(INCIDENCE_ENTRY).on(INCIDENCE_DATASET.ID.eq(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID))
            .groupBy(INCIDENCE_DATASET.NAME)
            .orderBy(INCIDENCE_DATASET.NAME)
            .fetch();

    List<String> names = datasetNames.map(Record1::value1);
    return names;
	}

	//code from air quality example of how to get dataset info to be viewed as table
// 		/**
// 	 * @param request - expected to contain id param
// 	 * @param response
// 	 * @param optional
// 	 * @return Single air quality layer definition as json string 
// 	 */
// 	public static Object getIncidenceDataset(Request request, Response response, Optional<UserProfile> userProfile) {
		
// 		Integer id;
// 		try {
// 			id = Integer.valueOf(request.params("id"));
// 		} catch (NumberFormatException e) {
// 			e.printStackTrace();
// 			return CoreApi.getErrorResponseInvalidId(request, response);
// 		}
		
// 		Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> aqRecord = getAirQualityLayerDefinition(id, userProfile);
// 		response.type("application/json");
// 		if(aqRecord == null) {
// 			return CoreApi.getErrorResponseNotFound(request, response);
// 		} else {
// 			return aqRecord.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
// 		}
// 	}
// /**
// 	 * 
// 	 * @param id
// 	 * @param userProfile
// 	 * @return a representation of an air quality layer definition.
// 	 */
// 	@SuppressWarnings("unchecked")
// 	public static @Nullable Record16<Integer, String, String, Short, Integer, Integer, String, String, String, String, String, LocalDateTime, String, String, String, JSON> getAirQualityLayerDefinition(Integer id, Optional<UserProfile> userProfile) {
// 		String userId = userProfile.get().getId();
// 		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
// 		Table<Record13<Integer, Integer, String, Integer, String, Integer, String, Integer, Double, Double, Double, Double, Double>> metricStatistics = create.select(
// 				AIR_QUALITY_LAYER_METRICS.AIR_QUALITY_LAYER_ID,
// 				AIR_QUALITY_LAYER_METRICS.METRIC_ID,
// 				POLLUTANT_METRIC.NAME.as("metric_name"),
// 				AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID,
// 				SEASONAL_METRIC.NAME.as("seasonal_metric_name"),
// 				AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID,
// 				STATISTIC_TYPE.NAME.as("annual_statistic_name"),
// 				AIR_QUALITY_LAYER_METRICS.CELL_COUNT,
// 				AIR_QUALITY_LAYER_METRICS.MIN_VALUE,
// 				AIR_QUALITY_LAYER_METRICS.MAX_VALUE,
// 				AIR_QUALITY_LAYER_METRICS.MEAN_VALUE,
// 				AIR_QUALITY_LAYER_METRICS.PCT_2_5,
// 				AIR_QUALITY_LAYER_METRICS.PCT_97_5)
// 				.from(AIR_QUALITY_LAYER_METRICS)
// 				.join(POLLUTANT_METRIC).on(POLLUTANT_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.METRIC_ID))
// 				.leftJoin(SEASONAL_METRIC).on(SEASONAL_METRIC.ID.eq(AIR_QUALITY_LAYER_METRICS.SEASONAL_METRIC_ID))
// 				.leftJoin(STATISTIC_TYPE).on(STATISTIC_TYPE.ID.eq(AIR_QUALITY_LAYER_METRICS.ANNUAL_STATISTIC_ID))
// 				.asTable("metric_statistics");
		

// 		return create.select(
// 				AIR_QUALITY_LAYER.ID, 
// 				AIR_QUALITY_LAYER.NAME,
// 				AIR_QUALITY_LAYER.USER_ID,
// 				AIR_QUALITY_LAYER.SHARE_SCOPE,
// 				AIR_QUALITY_LAYER.GRID_DEFINITION_ID,
// 				AIR_QUALITY_LAYER.POLLUTANT_ID,
// 				AIR_QUALITY_LAYER.AQ_YEAR,
// 				AIR_QUALITY_LAYER.DESCRIPTION,
// 				AIR_QUALITY_LAYER.SOURCE,
// 				AIR_QUALITY_LAYER.DATA_TYPE,
// 				AIR_QUALITY_LAYER.FILENAME,
// 				AIR_QUALITY_LAYER.UPLOAD_DATE,
// 				POLLUTANT.NAME.as("pollutant_name"), 
// 				POLLUTANT.FRIENDLY_NAME.as("pollutant_friendly_name"),
// 				GRID_DEFINITION.NAME.as("grid_definition_name"),
// 				DSL.jsonArrayAgg(DSL.jsonbObject(
// 						metricStatistics.field("metric_id"),
// 						metricStatistics.field("metric_name"),
// 						metricStatistics.field("seasonal_metric_id"),
// 						metricStatistics.field("seasonal_metric_name"),
// 						metricStatistics.field("annual_statistic_id"),
// 						metricStatistics.field("annual_statistic_name"),
// 						metricStatistics.field("cell_count"),
// 						metricStatistics.field("min_value"),
// 						metricStatistics.field("max_value"),
// 						metricStatistics.field("mean_value"),
// 						metricStatistics.field("pct_2_5"),
// 						metricStatistics.field("pct_97_5")
// 						)).as("metric_statistics")
// 				)
// 		.from(AIR_QUALITY_LAYER)
// 		.join(metricStatistics).on(((Field<Integer>)metricStatistics.field("air_quality_layer_id")).eq(AIR_QUALITY_LAYER.ID))
// 		.join(POLLUTANT).on(POLLUTANT.ID.eq(AIR_QUALITY_LAYER.POLLUTANT_ID))				
// 		.join(GRID_DEFINITION).on(GRID_DEFINITION.ID.eq(AIR_QUALITY_LAYER.GRID_DEFINITION_ID))
// 		.where(AIR_QUALITY_LAYER.ID.eq(id))
// 		.and(AIR_QUALITY_LAYER.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(AIR_QUALITY_LAYER.USER_ID.eq(userId)).or(CoreApi.isAdmin(userProfile) ? DSL.trueCondition() : DSL.falseCondition()))
// 		.groupBy(AIR_QUALITY_LAYER.ID
// 				, AIR_QUALITY_LAYER.NAME
// 				, AIR_QUALITY_LAYER.USER_ID
// 				, AIR_QUALITY_LAYER.SHARE_SCOPE
// 				, AIR_QUALITY_LAYER.GRID_DEFINITION_ID
// 				, AIR_QUALITY_LAYER.POLLUTANT_ID
// 				, AIR_QUALITY_LAYER.AQ_YEAR
// 				, AIR_QUALITY_LAYER.DESCRIPTION
// 				, AIR_QUALITY_LAYER.SOURCE
// 				, AIR_QUALITY_LAYER.DATA_TYPE
// 				, AIR_QUALITY_LAYER.FILENAME
// 				, AIR_QUALITY_LAYER.UPLOAD_DATE				
// 				, POLLUTANT.NAME
// 				, POLLUTANT.FRIENDLY_NAME
// 				, GRID_DEFINITION.NAME
				
// 				)
// 		.fetchOne();
// 	}

	public static Object postIncidenceData(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		String incidenceName;
		Integer gridId;
		String aqYear;
		String description;
		String source;
		String filename;
		LocalDateTime uploadDate;
		
		try{
			incidenceName= ApiUtil.getMultipartFormParameterAsString(request, "name");
			gridId = ApiUtil.getMultipartFormParameterAsInteger(request, "gridId");
			aqYear = ApiUtil.getMultipartFormParameterAsString(request, "aqYear");
			description = ApiUtil.getMultipartFormParameterAsString(request, "description");
			source = ApiUtil.getMultipartFormParameterAsString(request, "source");
			filename = ApiUtil.getMultipartFormParameterAsString(request, "filename");
			uploadDate = ApiUtil.getMultipartFormParameterAsLocalDateTime(request, "uploadDate", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		
		//Validate csv file
		String errorMsg= ""; //stores more detailed info. Not used in report for now but may need in the future?
		ValidationMessage validationMsg = new ValidationMessage();

				
		if(incidenceName == null ||  gridId == null) {
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Missing one or more required parameters: name, gridId."));
			return transformValMsgToJSON(validationMsg);
		}

		// //step 0: make sure incidenceName is not the same as any existing ones
		
		List<String>incidenceNames = getAllIncidencePrevalenceDatasetNames();
		if (incidenceNames.contains(incidenceName.toLowerCase())) {
			validationMsg.success = false;
			validationMsg.messages.add(new ValidationMessage.Message("error","An incidence dataset " + incidenceName + " already exists. Please enter a different name."));
			response.type("application/json");
			return transformValMsgToJSON(validationMsg);
		}
		
		IncidenceDatasetRecord incRecord=null;
		IncidenceEntryRecord entryRecord=null;
		int columnIdx=-999;
		int rowIdx=-999;
		int endpointGroupIdx=-999;
		int endpointIdx=-999;
		int raceIdx=-999;
		int genderIdx=-999;
		int ethnicityIdx=-999;
		int startAgeIdx=-999;
		int endAgeIdx=-999;
		int typeIdx=-999;
		int valueIdx=-999;
		
		Map<String, Integer> raceIdLookup = new HashMap<>();		
		Map<String, Integer> ethnicityIdLookup = new HashMap<>();		
		Map<String, Integer> genderIdLookup = new HashMap<>();
		HashMap<String,Map<String,Integer>> endpointIdLookup = new HashMap<String,Map<String,Integer>>();
		Map<String, Integer> endpointGroupIdLookup = new HashMap<>();

		
		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));				

			String[] record;
			
			//step 1: verify column names 
			// Read the header
			// allow either "column" or "col"; "values" or "value"
			// todo: warn or abort when both "column" and "col" exist.
			record = csvReader.readNext();
			for(int i=0; i < record.length; i++) {
				switch(record[i].toLowerCase().replace(" ", "")) {
				case "column":					
					if(columnIdx==-999) {
						columnIdx=i;
					}
					else {
						validationMsg.success = false;
						ValidationMessage.Message msg = new ValidationMessage.Message();
						msg.message = "File has both 'col' and 'column' fields.";
						msg.type = "error";
						validationMsg.messages.add(msg);
					}
					break;
				case "col":
					if(columnIdx==-999) {
						columnIdx=i;
					}
					else {
						validationMsg.success = false;
						ValidationMessage.Message msg = new ValidationMessage.Message();
						msg.message = "File has both 'col' and 'column' fields";
						msg.type = "error";
						validationMsg.messages.add(msg);
					}
					break;
				case "row":
					rowIdx=i;
					break;
				case "endpoint":
					endpointIdx=i;
					break;
				case "endpointgroup":
					endpointGroupIdx=i;
					break;
				case "race":
					raceIdx=i;
					break;
				case "gender":
					genderIdx=i;
					break;
				case "ethnicity":
					ethnicityIdx=i;
					break;
				case "startage":
					startAgeIdx=i;
					break;
				case "endage":
					endAgeIdx=i;
					break;
				case "type":
					typeIdx=i;
					break;
				case "value":
					valueIdx=i;
					break;	
				default:
					System.out.println(record[i].toLowerCase().replace(" ", ""));
				}
			}
			String tmp = IncidenceUtil.validateModelColumnHeadings(columnIdx, rowIdx, endpointGroupIdx, endpointIdx, raceIdx, genderIdx, ethnicityIdx, startAgeIdx, endAgeIdx, typeIdx, valueIdx);
			if(tmp.length() > 0) {
				log.debug("end age index is :" + endAgeIdx);

				log.debug("incidence dataset posted - columns are missing: " + tmp);
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following columns are missing: " + tmp;
				msg.type = "error";
				validationMsg.messages.add(msg);
				response.type("application/json");
				return transformValMsgToJSON(validationMsg);
			}
			
			ethnicityIdLookup = IncidenceUtil.getEthnicityIdLookup();
			raceIdLookup = IncidenceUtil.getRaceIdLookup();
			genderIdLookup = IncidenceUtil.getGenderIdLookup();
			endpointGroupIdLookup = IncidenceUtil.getEndpointGroupIdLookup();
			// HashMap<String,HashMap<String,Integer>> endpointIdLookup = new HashMap<String,HashMap<String,Integer>>();
		

			// System.out.println(record[endpointGroupIdx]);
			// System.out.println(endpointGroupIdLookup.get(record[endpointGroupIdx]));
			// endpointIdLookup = IncidenceUtil.getEndpointIdLookup(endpointGroupIdLookup.get(record[endpointGroupIdx]));
			

			
		// 	//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
		// 	//step 2: make sure file has > 0 rows. Check rowCount after while loop.
			int rowCount = 0;
			int countColTypeError = 0;
			int countRowTypeError = 0;
			int countMissingRace = 0;
			int countMissingEthnicity = 0;
			int countMissingGender = 0;
			int countMissingEndpoint = 0;
			int countMissingEndpointGroup= 0;

			int countValueTypeError = 0;
			int countValueError = 0;
			List<String> lstUndefinedEthnicities = new ArrayList<String>();
			List<String> lstUndefinedRaces = new ArrayList<String>();
			List<String> lstUndefinedGenders = new ArrayList<String>();
			List<String> lstUndefinedEndpoints = new ArrayList<String>();
			List<String> lstUndefinedEndpointGroups = new ArrayList<String>();


			List<String> lstDupMetricCombo = new ArrayList<String>();
			
			Map<String, Integer> dicUniqueRecord = new HashMap<String,Integer>();	
			
			while ((record = csvReader.readNext()) != null) {				
				rowCount ++;
				//endpoint id hashmap is a nested dictionary with the outer key being endpoint groups and values being hashmaps of endpoint names to ids
				String endpointGroupName = record[endpointGroupIdx].toLowerCase();
				if (!endpointIdLookup.containsKey(endpointGroupName)){
					Integer endpointGroupId = endpointGroupIdLookup.get(endpointGroupName);
					//endpoint group id is a short in endpoint data but an Integer in endpoint group data
					short shortEndpointGroupId = (short) (int) endpointGroupId;
					endpointIdLookup.put(endpointGroupName, IncidenceUtil.getEndpointIdLookup(shortEndpointGroupId));
				}


				// Make sure this metric exists in the db. If not, update the corresponding error array to return useful error message
				String str = "";

				str = record[ethnicityIdx];
				if(str == "") {
					countMissingEthnicity ++;
				}
				if(!ethnicityIdLookup.containsKey(str.toLowerCase() ) ) {
					if (!lstUndefinedEthnicities.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEthnicities.add(String.valueOf(str.toLowerCase()));
					}
				}
				
				str = record[raceIdx];
				if(str == "") {
					countMissingRace ++;
				}
				if(!raceIdLookup.containsKey(str.toLowerCase())) {
					if (!lstUndefinedRaces.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedRaces.add(String.valueOf(str.toLowerCase()));
					}
				}

				
				str= record[genderIdx];
				if(str == "") {
					countMissingGender ++;
				}
				if(!genderIdLookup.containsKey(str.toLowerCase()) ) {
					if (!lstUndefinedGenders.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedGenders.add(String.valueOf(str.toLowerCase()));
					}
				}

				str = record[endpointIdx];
				if(str == "") {
					countMissingEndpoint ++;
				}
				if(!endpointIdLookup.get(endpointGroupName).containsKey(str.toLowerCase()) ) {
					if (!lstUndefinedEndpoints.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEndpoints.add(String.valueOf(str.toLowerCase()));
					}
				}

				str = record[endpointGroupIdx];
				if(str == "") {
					countMissingEndpointGroup ++;
				}
				if(!endpointGroupIdLookup.containsKey(str.toLowerCase()) ) {
					if (!lstUndefinedEndpointGroups.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEndpointGroups.add(String.valueOf(str.toLowerCase()));
					}
				}
						
				
		// 		//step 3: Verify data types for each field
				//column is required and should be an integer
				str = record[columnIdx];
				if(str=="" || !str.matches("-?\\d+")) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "column value " + str + " is not a valid integer." + "\r\n";
					countColTypeError++;
				}	
				//row is required and should be an integer
				str = record[rowIdx];
				//question: or use Integer.parseInt(str)??
				if(str=="" || !str.matches("-?\\d+")) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "row value " + str + " is not a valid integer."+ "\r\n";
					countRowTypeError++;
				}	
				//value/values should be a double and >= 0
				str = record[valueIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Value " + str + " is not a valid as it is less than 0."+ "\r\n";
						countValueTypeError ++;
					}
				}
				catch(NumberFormatException e){
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Value " + str + " is not a valid double."+ "\r\n";
					countValueError ++;
				}
		
		//check that we don't have duplicate records for a given categorization and row/col
				str = record[columnIdx].toString() 
						+ "~" + record[rowIdx].toLowerCase() 
						+ "~" + record[endpointGroupIdx].toLowerCase() 
						+ "~" + record[endpointIdx].toLowerCase() 
						+ "~" + record[raceIdx].toLowerCase()
						+ "~" + record[genderIdx].toLowerCase()
						+ "~" + record[ethnicityIdx].toLowerCase()
						+ "~" + record[startAgeIdx].toLowerCase()
						+ "~" + record[endAgeIdx].toLowerCase()
						+ "~" + record[typeIdx].toLowerCase()
						+ "~" + record[valueIdx].toLowerCase();
				if(!dicUniqueRecord.containsKey(str)) {
					dicUniqueRecord.put(str,rowCount + 1);
				}
				else {
					if(!lstDupMetricCombo.contains(str)) {
						lstDupMetricCombo.add(str);
					}
				}
			}	

			// //summarize validation message
			if(countColTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countColTypeError == 1) {
					strRecord = String.valueOf(countColTypeError) + " record has Column values not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countColTypeError) + " records have Column values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countRowTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countRowTypeError == 1) {
					strRecord = String.valueOf(countRowTypeError) + " record has Row values not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countRowTypeError) + " records have Row values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countValueTypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueTypeError == 1) {
					strRecord = String.valueOf(countValueTypeError) + " record has an incidence value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countValueTypeError) + " records have  incidence values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countValueError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueError == 1) {
					strRecord = String.valueOf(countValueError) + " record has";
				}
				else {
					strRecord = String.valueOf(countValueError) + " records have";
				}
				msg.message = strRecord + " incidence values below zero.";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEthnicity>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEthnicity == 1) {
					strRecord = String.valueOf(countMissingEthnicity) + " record is missing a Ethnicity value.";
				}
				else {
					strRecord = String.valueOf(countMissingEthnicity) + " records are missing Ethnicity values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEthnicities.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedEthnicities) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingRace>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingRace == 1) {
					strRecord = String.valueOf(countMissingRace) + " record is missing a Race value.";
				}
				else {
					strRecord = String.valueOf(countMissingEthnicity) + " records are missing Race values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedRaces.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedRaces) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingGender>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingGender == 1) {
					strRecord = String.valueOf(countMissingGender) + " record is missing a Gender value.";
				}
				else {
					strRecord = String.valueOf(countMissingGender) + " records are missing Gender values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedGenders.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedGenders) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpoint>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEndpoint == 1) {
					strRecord = String.valueOf(countMissingEndpoint) + " record is missing a Endpoint value.";
				}
				else {
					strRecord = String.valueOf(countMissingEthnicity) + " records are missing Endpoint values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpoints.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Endpoint values are not defined: " + String.join(",", lstUndefinedEndpoints) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countMissingEndpointGroup>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countMissingEthnicity == 1) {
					strRecord = String.valueOf(countMissingEndpointGroup) + " record is missing a Endpoint Group value.";
				}
				else {
					strRecord = String.valueOf(countMissingEndpointGroup) + " records are missing Endpoint Group values.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(lstUndefinedEndpointGroups.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Endpoint Group values are not defined: " + String.join(",", lstUndefinedEndpointGroups) + ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			
			if(lstDupMetricCombo.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Metric combinations are not unique: " + String.join(",", lstDupMetricCombo)+ ".";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			
			
			//---End of csv validation
			
		} catch (Exception e) {
			log.error("Error validating incidence file", e);
			response.type("application/json");
			//response.status(400);
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during validation of incidence file."));
			return transformValMsgToJSON(validationMsg);
		}
		
		
		Integer incidenceDatasetId = null;
		
		//import data
		try (InputStream is = request.raw().getPart("file").getInputStream()){
			CSVReader csvReader = new CSVReader (new InputStreamReader(is));
			String[] record;
			record = csvReader.readNext();
			
			//TODO: need to add user_id, sharing_status, file_name, and upload_date columns

			//Create the incidence record
			incRecord = DSL.using(JooqUtil.getJooqConfiguration())
			.insertInto(INCIDENCE_DATASET
					, INCIDENCE_DATASET.NAME
					, INCIDENCE_DATASET.GRID_DEFINITION_ID)
			.values(incidenceName,  gridId)
			.returning(INCIDENCE_DATASET.ID, INCIDENCE_DATASET.NAME,INCIDENCE_DATASET.GRID_DEFINITION_ID)
			.fetchOne();

			
			incidenceDatasetId = incRecord.value1();
			// log.debug("the id of the new dataset is " + incidenceDatasetId);

			InsertValuesStep5<IncidenceValueRecord, Integer, Long, Integer, Integer, Double> batch2 = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							INCIDENCE_VALUE,
							INCIDENCE_VALUE.INCIDENCE_ENTRY_ID,
							INCIDENCE_VALUE.GRID_CELL_ID,
							INCIDENCE_VALUE.GRID_COL,
							INCIDENCE_VALUE.GRID_ROW,
							INCIDENCE_VALUE.VALUE
						);
		
		Map<String, Integer> incidenceEntryIds= new HashMap<String,Integer>();

		while ((record = csvReader.readNext()) != null) {
			//use the hashmaps created from incidenceUtil to get the id of each column metric
				String endpointGroupName = record[endpointGroupIdx].toLowerCase();
				int endpointGroupId = endpointGroupIdLookup.get(endpointGroupName);
				int endpointId = endpointIdLookup.get(endpointGroupName).get(record[endpointIdx].toLowerCase());
				

				String raceName = record[raceIdx].toLowerCase();
				if (raceName.equals("")){
					raceName = null;
				}
				int raceId = raceIdLookup.get(raceName);

				String genderName = record[genderIdx].toLowerCase();
				if (genderName.equals("")){
					genderName = null;
				}
				int genderId = genderIdLookup.get(genderName);

				String ethnicityName = record[ethnicityIdx].toLowerCase();
				if (ethnicityName.equals("")){
					ethnicityName = null;
				}
				int ethnicityId = ethnicityIdLookup.get(ethnicityName);
				short startAge = Short.valueOf(record[startAgeIdx]);
				short endAge = Short.valueOf(record[endAgeIdx]);
				boolean prevalence = "prevalence".equalsIgnoreCase(record[typeIdx]);

				//check if there is a matching incidence_entry_id for the current row's metrics
				String entryQuery = String.format("incidence_dataset_id=%d and grid_definition_id=%d and endpoint_group_id=%d and endpoint_id=%d and race_id=%d and gender_id=%d and start_age=%d and end_age=%d and type='%s' and ethnicity_id=%d",
				incidenceDatasetId,
				gridId,
				endpointGroupId,
				endpointId,
				raceId,
				genderId,
				startAge,
				endAge,
				prevalence,
				ethnicityId
				);

				int incidenceEntryId = -999;
				//if the metric combo isn't in hashmap, add to database and updata hashmap
				if (!incidenceEntryIds.containsKey(entryQuery)){
					entryRecord = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							INCIDENCE_ENTRY, 
							INCIDENCE_ENTRY.INCIDENCE_DATASET_ID,
							// INCIDENCE_ENTRY.YEAR,
							INCIDENCE_ENTRY.ENDPOINT_GROUP_ID,
							INCIDENCE_ENTRY.ENDPOINT_ID,
							INCIDENCE_ENTRY.RACE_ID,
							INCIDENCE_ENTRY.GENDER_ID,
							INCIDENCE_ENTRY.START_AGE,
							INCIDENCE_ENTRY.END_AGE,
							INCIDENCE_ENTRY.PREVALENCE,
							INCIDENCE_ENTRY.ETHNICITY_ID
					)
					.values(				
						incidenceDatasetId, 
						endpointGroupId,
						endpointId,
						raceId,
						genderId,
						Short.valueOf(record[startAgeIdx]),
						Short.valueOf(record[endAgeIdx]),
						"prevalence".equalsIgnoreCase(record[typeIdx]),
						ethnicityId
					)
					.returning(INCIDENCE_ENTRY.ID,INCIDENCE_ENTRY.INCIDENCE_DATASET_ID,
							INCIDENCE_ENTRY.ENDPOINT_GROUP_ID,
							INCIDENCE_ENTRY.ENDPOINT_ID,
							INCIDENCE_ENTRY.RACE_ID,
							INCIDENCE_ENTRY.GENDER_ID,
							INCIDENCE_ENTRY.START_AGE,
							INCIDENCE_ENTRY.END_AGE,
							INCIDENCE_ENTRY.PREVALENCE,
							INCIDENCE_ENTRY.ETHNICITY_ID)
					.fetchOne();

					incidenceEntryId = entryRecord.value1();
					incidenceEntryIds.put(entryQuery, incidenceEntryId);
				}
				else{
					//otherwise just get the id
					incidenceEntryId = incidenceEntryIds.get(entryQuery);
				}
				
			

				batch2.values(
						incidenceEntryId,
						ApiUtil.getCellId(Integer.valueOf(record[columnIdx]), Integer.valueOf(record[rowIdx])),
						Integer.valueOf(record[columnIdx]),
						Integer.valueOf(record[rowIdx]),
						Double.valueOf(record[valueIdx])
				);
				
			}
			

			batch2.execute();

			
		
		} catch (Exception e) {
			log.error("Error importing incidence file", e);
			response.type("application/json");
		}
		
		response.type("application/json");
		validationMsg.success = true;
		return transformValMsgToJSON(validationMsg); 
	}

	
		/**
	 * Transforms a validation message into a JsonNode
	 * @param validationMessage
	 * @return the transformed validation message as a JsonNode.
	 */
	private static JsonNode transformValMsgToJSON(ValidationMessage validationMessage) {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode recordsJSON = null;
		//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			recordsJSON = mapper.valueToTree(validationMessage);
		} catch (Exception e) {
			log.error("Error converting validation message to JSON",e);
		} 
		
		return recordsJSON;
		
	}

}
