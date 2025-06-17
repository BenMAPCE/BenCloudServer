package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.AIR_QUALITY_LAYER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.ENDPOINT;
import static gov.epa.bencloud.server.database.jooq.data.Tables.ENDPOINT_GROUP;
import static gov.epa.bencloud.server.database.jooq.data.Tables.ETHNICITY;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GENDER;
import static gov.epa.bencloud.server.database.jooq.data.Tables.GET_INCIDENCE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.INCIDENCE_DATASET;
import static gov.epa.bencloud.server.database.jooq.data.Tables.INCIDENCE_ENTRY;
import static gov.epa.bencloud.server.database.jooq.data.Tables.INCIDENCE_VALUE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.RACE;
import static gov.epa.bencloud.server.database.jooq.data.Tables.TASK_CONFIG;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.io.input.BOMInputStream;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep5;
import org.jooq.JSONFormat;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record16;
import org.jooq.Record3;
import org.jooq.Record8;
import org.jooq.Result;
import org.jooq.SortOrder;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;

import gov.epa.bencloud.Constants;
import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.api.model.PopulationCategoryKey;
import gov.epa.bencloud.api.model.ValidationMessage;
import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.api.util.IncidenceUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetIncidenceRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceEntryRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.IncidenceValueRecord;
import gov.epa.bencloud.server.util.DataConversionUtil;
import gov.epa.bencloud.server.util.ParameterUtil;
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
		Integer[] arrRaceId = new Integer[2];
		arrRaceId[0] = raceId;
		if(raceId == 6) {
			arrRaceId[1] = 5;
		}
		Integer[] arrEthnicityId = new Integer[2];
		arrEthnicityId[0] = ethnicityId;
		if(ethnicityId == 4) {
			arrEthnicityId[1] = 3;
		}
		Integer[] arrGenderId = new Integer[2];
		arrGenderId[0] = genderId;
		if(genderId == 4) {
			arrGenderId[1] = 3;
		}

		Integer aqGridId = AirQualityApi.getAirQualityLayerGridId(hifTaskConfig.aqBaselineId);
		
		//If the crosswalk isn't there, create it now
		CrosswalksApi.ensureCrosswalkExists(getIncidenceGridDefinitionId(incPrevId),aqGridId);
		

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
				aqGridId)
				.intoGroups(GET_INCIDENCE.GRID_CELL_ID);
		
		//If there are no incidence records in the dataset for the given race/gender/ethnicity, get records for ALL/ALL/ALL
		//The incidence selection logic will default to a dataset with ALL/ALL/ALL in this scenario
		//Without this, the baseline incidence will = 0 and no result will be calculated
		if(incRecords.entrySet().size() == 0) {
			arrRaceId[0] = 6;
			arrEthnicityId[0] = 4;
			arrGenderId[0] = 4;
			incRecords = Routines.getIncidence(JooqUtil.getJooqConfiguration(), 
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
				aqGridId)
				.intoGroups(GET_INCIDENCE.GRID_CELL_ID);
		}

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
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all incidence AND prevalence datasets.
	 */
	public static Object getAllIncidencePrevalenceDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		boolean showAll;
		try {
			showAll = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("showAll"), false);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		
		String userId = userProfile.get().getId();
	
		Condition filterCondition = DSL.trueCondition();
		Condition userFilterCondition = DSL.trueCondition();

		if(!showAll || !CoreApi.isAdmin(userProfile)) {
			userFilterCondition = userFilterCondition.and(INCIDENCE_DATASET.USER_ID.eq(userId));
			userFilterCondition = userFilterCondition.or(INCIDENCE_DATASET.SHARE_SCOPE.isTrue());
			filterCondition = filterCondition.and(userFilterCondition);
		}

		Result<Record8<String, Integer, Integer, Integer[], String, Short, String, LocalDateTime >> records = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID,
						DSL.arrayAggDistinct(INCIDENCE_ENTRY.YEAR).orderBy(INCIDENCE_ENTRY.YEAR).as("years"),
						INCIDENCE_DATASET.USER_ID,
						INCIDENCE_DATASET.SHARE_SCOPE,
						INCIDENCE_DATASET.FILENAME,
						INCIDENCE_DATASET.UPLOAD_DATE
						)
				.from(INCIDENCE_DATASET)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_DATASET.ID.eq(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID))
				.where(filterCondition)
				.groupBy(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID,
						INCIDENCE_DATASET.USER_ID,
						INCIDENCE_DATASET.SHARE_SCOPE,
						INCIDENCE_DATASET.FILENAME,
						INCIDENCE_DATASET.UPLOAD_DATE
						)
				.orderBy(INCIDENCE_DATASET.NAME)
				.fetch();
		
		response.type("application/json");
		return records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	/**
	 * 
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all incidence datasets.
	 */
	public static Object getAllIncidenceDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(request, response, userProfile, false);
	}


	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all prevalance datasets.
	 */
	public static Object getAllPrevalenceDatasets(Request request, Response response, Optional<UserProfile> userProfile) {
		return getAllIncidencePrevalenceDatasets(request, response, userProfile, true);
	}
	

	/**
	 * 
	 * @param response
	 * @param prevalence true if prevalance, false if incidence
	 * @return a JSON representation of all incidence or prevalance datasets.
	 */
	public static Object getAllIncidencePrevalenceDatasets(Request request, Response response, Optional<UserProfile> userProfile, boolean prevalence) {
		Result<Record8<String, Integer, Integer, Integer[], String, Short, String, LocalDateTime >> records = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID,
						DSL.arrayAggDistinct(INCIDENCE_ENTRY.YEAR).orderBy(INCIDENCE_ENTRY.YEAR).as("years"),
						INCIDENCE_DATASET.USER_ID,
						INCIDENCE_DATASET.SHARE_SCOPE,
						INCIDENCE_DATASET.FILENAME,
						INCIDENCE_DATASET.UPLOAD_DATE
						)
				.from(INCIDENCE_DATASET)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_DATASET.ID.eq(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID))
				.where(INCIDENCE_ENTRY.PREVALENCE.eq(prevalence))
				.groupBy(INCIDENCE_DATASET.NAME,
						INCIDENCE_DATASET.ID,
						INCIDENCE_DATASET.GRID_DEFINITION_ID,
						INCIDENCE_DATASET.USER_ID,
						INCIDENCE_DATASET.SHARE_SCOPE,
						INCIDENCE_DATASET.FILENAME,
						INCIDENCE_DATASET.UPLOAD_DATE
						)
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
		
		if (record == null) {
			switch (id) {
				case 1:
					return "County (2010)";
				case 2:
					return "County, race-stratified (2010)";
				case 4:
					return "County, ethnicity-stratified (2010)";
				case 5:
					return "County, ethnicity-adjusted race-stratified (2010)";
				default:
					return "dataset removed";
			}
		}
		
		return record.value1();
	}

	/**
	 * 
	 * @param id incidence dataset id
	 * @return the grid definition ID of a given incidence dataset.
	 */
	public static Integer getIncidenceGridDefinitionId(int id) {

		Record1<Integer> record = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.GRID_DEFINITION_ID)
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

	/**
	 *  @param userId
	 *  @return the names of all the incidence or prevalence datasets by user, return all in lower cases.
	 */
	public static List<String> getAllIncidencePrevalenceDatasetNamesByUser(String userId) {
		if(userId == null) {
            return null;
        }
		Result<Record1<String>> datasetNames = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(INCIDENCE_DATASET.NAME))
				.from(INCIDENCE_DATASET)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_DATASET.ID.eq(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID))
				.where(INCIDENCE_DATASET.USER_ID.equal(userId).or(INCIDENCE_DATASET.SHARE_SCOPE.equal((short) 1)))
				.groupBy(INCIDENCE_DATASET.NAME)
				.orderBy(INCIDENCE_DATASET.NAME)
				.fetch();
	
		List<String> names = datasetNames.map(Record1::value1);
		return names;
		}

/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the incidence dataset details for a given incidence dataset (incidence dataset id is a request parameter).
	 */
	public static Object getIncidenceDatasetDetails(Request request, Response response, Optional<UserProfile> userProfile) {
		
		String userId = userProfile.get().getId();
		
		int id;
		int page;
		int rowsPerPage;
		String sortBy;
		boolean descending;
		String filter;
		int year;

		// Get response output stream
		OutputStream responseOutputStream;
		ZipOutputStream zipStream;

		try {
			responseOutputStream = response.raw().getOutputStream();
			
			// Stream .ZIP file to response
			zipStream = new ZipOutputStream(responseOutputStream);
		} catch (java.io.IOException e1) {
			return CoreApi.getErrorResponse(request, response, 400, "Error getting output stream");
		}

		try {
			id = Integer.valueOf(request.params("id"));
			page = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("page"), 1);
			rowsPerPage = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("rowsPerPage"), 10);
			sortBy = ParameterUtil.getParameterValueAsString(request.raw().getParameter("sortBy"), "");
			descending = ParameterUtil.getParameterValueAsBoolean(request.raw().getParameter("descending"), false);
			filter = ParameterUtil.getParameterValueAsString(request.raw().getParameter("filter"), "");
			year = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("year"), 0);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}
		//System.out.println("id: " + id);
		//System.out.println("filter: " + filter);
		//System.out.println("rowsPerPage: " + rowsPerPage);

		Condition filterCondition = DSL.trueCondition();
		Condition incidenceDatasetCondition = DSL.trueCondition();
		Condition yearFilterCondition = DSL.trueCondition();
				
		if (id != 0) {
			incidenceDatasetCondition = DSL.field(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID).eq(id);
			filterCondition = filterCondition.and(incidenceDatasetCondition);
		}

		if (year != 0) {
			yearFilterCondition = DSL.field(INCIDENCE_ENTRY.YEAR).eq(year);
			filterCondition = filterCondition.and(yearFilterCondition);
		}

		if (!"".equals(filter)) {
			filterCondition = filterCondition.and(buildIncidenceCellsFilterCondition(filter));
		}
		filterCondition = filterCondition.and(INCIDENCE_DATASET.SHARE_SCOPE.eq(Constants.SHARING_ALL).or(INCIDENCE_DATASET.USER_ID.eq(userId)).or(CoreApi.isAdmin(userProfile) ? DSL.trueCondition() : DSL.falseCondition()));
	
		List<OrderField<?>> orderFields = new ArrayList<>();
		
		//System.out.println("sortBy: " + sortBy);
		
		setIncidenceCellsSortOrder(sortBy, descending, orderFields);


		Integer filteredRecordsCount = 
				DSL.using(JooqUtil.getJooqConfiguration()).select(DSL.count())
				.from(INCIDENCE_VALUE)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID.eq(INCIDENCE_ENTRY.ID))
				.join(INCIDENCE_DATASET).on(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(INCIDENCE_DATASET.ID))
				.leftJoin(ENDPOINT).on(INCIDENCE_ENTRY.ID.eq(ENDPOINT.ID))
				.leftJoin(ENDPOINT_GROUP).on(INCIDENCE_ENTRY.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.leftJoin(RACE).on(INCIDENCE_ENTRY.RACE_ID.eq(RACE.ID))
				.leftJoin(GENDER).on(INCIDENCE_ENTRY.GENDER_ID.eq(GENDER.ID))
				.leftJoin(ETHNICITY).on(INCIDENCE_ENTRY.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(filterCondition)
				.fetchOne(DSL.count());

		//System.out.println("filteredRecordsCount: " + filteredRecordsCount);

		Result<Record16<Integer, Integer, String, String, String, String, String, Integer, Short, Short, String, String, String, Double, String, Double>> incRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(
						INCIDENCE_VALUE.GRID_COL.as("Column"),
						INCIDENCE_VALUE.GRID_ROW.as("Row"),
						ENDPOINT.NAME.as("Health Effect"),
						ENDPOINT_GROUP.NAME.as("Health Effect Group"),
						RACE.NAME.as("Race"),
						GENDER.NAME.as("Gender"),
						ETHNICITY.NAME.as("Ethnicity"),
						INCIDENCE_ENTRY.YEAR.as("Year"),
						INCIDENCE_ENTRY.START_AGE.as("Start Age"),
						INCIDENCE_ENTRY.END_AGE.as("End Age"),
						DSL.when(INCIDENCE_ENTRY.PREVALENCE.eq(true), "Prevalence")
						.when(INCIDENCE_ENTRY.PREVALENCE.eq(false), "Incidence").as("Type"),
						INCIDENCE_ENTRY.TIMEFRAME.as("Timeframe"),
						INCIDENCE_ENTRY.UNITS.as("Units"),
						INCIDENCE_VALUE.VALUE.as("Value"),
						INCIDENCE_ENTRY.DISTRIBUTION.as("Distribution"),
						INCIDENCE_ENTRY.STANDARD_ERROR.as("Standard Error")
						)
				.from(INCIDENCE_VALUE)
				.join(INCIDENCE_ENTRY).on(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID.eq(INCIDENCE_ENTRY.ID))
				.join(INCIDENCE_DATASET).on(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(INCIDENCE_DATASET.ID))
				.leftJoin(ENDPOINT).on(INCIDENCE_ENTRY.ENDPOINT_ID.eq(ENDPOINT.ID))
				.leftJoin(ENDPOINT_GROUP).on(INCIDENCE_ENTRY.ENDPOINT_GROUP_ID.eq(ENDPOINT_GROUP.ID))
				.leftJoin(RACE).on(INCIDENCE_ENTRY.RACE_ID.eq(RACE.ID))
				.leftJoin(GENDER).on(INCIDENCE_ENTRY.GENDER_ID.eq(GENDER.ID))
				.leftJoin(ETHNICITY).on(INCIDENCE_ENTRY.ETHNICITY_ID.eq(ETHNICITY.ID))
				.where(filterCondition)
				.orderBy(orderFields)
				.offset((page * rowsPerPage) - rowsPerPage)
				.limit(rowsPerPage)
				.fetch();
		
		Record1<String> datasetInfo = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INCIDENCE_DATASET.NAME)
				.from(INCIDENCE_DATASET)
				.where(INCIDENCE_DATASET.ID.eq(id))
				.fetchOne();

		if(request.headers("Accept").equalsIgnoreCase("application/zip")) {
			String fileName = createFilename(datasetInfo.get(INCIDENCE_DATASET.NAME));
			response.type("application/zip");
			response.header("Content-Disposition", "attachment; filename="+ fileName + ".zip");
			response.header("Access-Control-Expose-Headers", "Content-Disposition");
			
			try {
				zipStream.putNextEntry(new ZipEntry(fileName + ".csv"));
				incRecords.formatCSV(zipStream);
				zipStream.closeEntry();
				zipStream.close();
				responseOutputStream.flush();
				return null;
			} catch (Exception e) {
				return CoreApi.getErrorResponse(request, response, 400, "Error creating export file");
			} 
		} else {

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode data = mapper.createObjectNode();
			
			data.put("filteredRecordsCount", filteredRecordsCount);

			try {
				JsonFactory factory = mapper.getFactory();
				JsonParser jp = factory.createParser(
						incRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT)));
				JsonNode actualObj = mapper.readTree(jp);
				data.set("records", actualObj);
			} catch (JsonParseException e) {
				log.error("Error parsing JSON",e);
			} catch (JsonProcessingException e) {
				log.error("Error processing JSON",e);
			} catch (IOException e) {
				log.error("IO Exception", e);
			}

			response.type("application/json");
			return data;
		}
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of the incidence dataset years for a given incidence dataset (incidence dataset id is a request parameter).
	 */
	public static Object getIncidenceDatasetYears(Request request, Response response, Optional<UserProfile> userProfile) {

		int incidenceDatasetId;
		try{
			incidenceDatasetId = ParameterUtil.getParameterValueAsInteger(request.raw().getParameter("incidenceDatasetId"), 0);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		}

		Result<?> years = DSL.using(JooqUtil.getJooqConfiguration())
			.selectDistinct(INCIDENCE_ENTRY.YEAR)
			.from(INCIDENCE_ENTRY)
			.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(incidenceDatasetId))
			.fetch();
		
		response.type("application/json");
		return years.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}

	public static Object postIncidenceData(Request request, Response response, Optional<UserProfile> userProfile) {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
		String incidenceName;
		Integer gridId;
		String filename;
		LocalDateTime uploadDate;
		
		try{
			incidenceName= ApiUtil.getMultipartFormParameterAsString(request, "name");
			gridId = ApiUtil.getMultipartFormParameterAsInteger(request, "gridId");
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

		String userId = userProfile.get().getId();

		// //step 0: make sure incidenceName is not the same as any existing ones
		
		List<String>incidenceNames = getAllIncidencePrevalenceDatasetNamesByUser(userId);
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
		int yearIdx=-999;
		int startAgeIdx=-999;
		int endAgeIdx=-999;
		int typeIdx=-999;
		int timeFrameIdx=-999;
		int unitsIdx=-999;
		int valueIdx=-999;
		int distributionIdx=-999;
		int standardErrorIdx=-999;
		
		Map<String, Integer> raceIdLookup = new HashMap<>();		
		Map<String, Integer> ethnicityIdLookup = new HashMap<>();		
		Map<String, Integer> genderIdLookup = new HashMap<>();
		HashMap<String,Map<String,Integer>> endpointIdLookup = new HashMap<String,Map<String,Integer>>();
		Map<String, Integer> endpointGroupIdLookup = new HashMap<>();

		
		try (InputStream is = request.raw().getPart("file").getInputStream()) {
			BOMInputStream bis = new BOMInputStream(is, false);
			CSVReader csvReader = new CSVReader (new InputStreamReader(bis));				

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
				case "healtheffect":
					endpointIdx=i;
					break;
				case "healtheffectgroup":
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
				case "year":
					yearIdx=i;
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
				case "timeframe":
					timeFrameIdx=i;
					break;	
				case "units":
					unitsIdx=i;
					break;			
				case "value":
					valueIdx=i;
					break;	
				case "distribution":
					distributionIdx=i;
					break;
				case "standarderror":
					standardErrorIdx=i;
					break;		
				default:
					System.out.println(record[i].toLowerCase().replace(" ", ""));
				}
			}
			String tmp = IncidenceUtil.validateModelColumnHeadings(columnIdx, rowIdx, endpointGroupIdx, endpointIdx, raceIdx, genderIdx, ethnicityIdx, yearIdx, startAgeIdx, endAgeIdx, typeIdx, timeFrameIdx, unitsIdx, valueIdx, distributionIdx, standardErrorIdx);
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
			

			
		// 	//We might also need to clean up the header. Or, maybe we should make this a transaction?
			
		// 	//step 2: make sure file has > 0 rows. Check rowCount after while loop.
			int rowCount = 0;
			int countColTypeError = 0;
			int countRowTypeError = 0;
			int countYearTypeError = 0;

			int countMissingEthnicity = 0;
			int countMissingEndpoint = 0;
			int countMissingEndpointGroup = 0;

			int countValueTypeError = 0;
			int countValueError = 0;
			int countSETypeError = 0;
			int countSEError = 0;

			List<String> lstUndefinedEthnicities = new ArrayList<String>();
			List<String> lstUndefinedRaces = new ArrayList<String>();
			List<String> lstUndefinedGenders = new ArrayList<String>();
			List<String> lstUndefinedYears = new ArrayList<String>();
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

				//TODO: Update this validation code when we add lookup tables for timeframe, units, and/or distribution
				// Make sure this metric exists in the db. If not, update the corresponding error array to return useful error message
				String str = "";

				str = record[ethnicityIdx];
				if(!ethnicityIdLookup.containsKey(str.toLowerCase() ) && !str.equals("")) {
					if (!lstUndefinedEthnicities.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedEthnicities.add(String.valueOf(str.toLowerCase()));
					}
				}
				
				str = record[raceIdx];
				if(!raceIdLookup.containsKey(str.toLowerCase()) && !str.equals("")) {
					if (!lstUndefinedRaces.contains(String.valueOf(str.toLowerCase()))) {
						lstUndefinedRaces.add(String.valueOf(str.toLowerCase()));
					}
				}

				
				str= record[genderIdx];
				if(!genderIdLookup.containsKey(str.toLowerCase()) && !str.equals("")) {
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
				} catch(NumberFormatException e){
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Value " + str + " is not a valid double."+ "\r\n";
					countValueError ++;
				}

				//year is required and should be an integer
				str = record[yearIdx];
				if(str=="" || !str.matches("-?\\d+")) {
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "column value " + str + " is not a valid integer." + "\r\n";
					countYearTypeError++;
				}	

				//standard error should be a double and >= 0
				str = record[valueIdx];
				try {
					double dbl = Double.parseDouble(str);
					if (dbl<0) {
						//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Standard Error " + str + " is not a valid as it is less than 0."+ "\r\n";
						countSETypeError ++;
					}
				}
				catch(NumberFormatException e){
					//errorMsg +="record #" + String.valueOf(rowCount + 1) + ": " +  "Standard Error " + str + " is not a valid double."+ "\r\n";
					countSEError ++;
				}

		
		//check that we don't have duplicate records for a given categorization and row/col
		//update to include timeframe, units, distribution, and SE?
				str = record[columnIdx].toString() 
						+ "~" + record[rowIdx].toLowerCase() 
						+ "~" + record[endpointGroupIdx].toLowerCase() 
						+ "~" + record[endpointIdx].toLowerCase() 
						+ "~" + record[raceIdx].toLowerCase()
						+ "~" + record[genderIdx].toLowerCase()
						+ "~" + record[ethnicityIdx].toLowerCase()
						+ "~" + record[yearIdx].toLowerCase()
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

			//summarize validation message
			//can probably remove all of the error checks for missing gender, ethnicity, race values because those can be null and would only be "" if cell doesn't exist?
			if(countColTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countColTypeError == 1) {
					strRecord = String.valueOf(countColTypeError) + " record has a Column value that is not a valid integer.";
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
					strRecord = String.valueOf(countRowTypeError) + " record has a Row value that is not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countRowTypeError) + " records have Row values that are not valid integers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}

			if(countYearTypeError>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countYearTypeError == 1) {
					strRecord = String.valueOf(countYearTypeError) + " record has a Year value that is not a valid integer.";
				}
				else {
					strRecord = String.valueOf(countYearTypeError) + " records have Year values that are not valid integers.";
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
					strRecord = String.valueOf(countValueTypeError) + " records have incidence values that are not valid numbers.";
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

			if(countSETypeError > 0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueTypeError == 1) {
					strRecord = String.valueOf(countSETypeError) + " record has a standard error value that is not a valid number.";
				}
				else {
					strRecord = String.valueOf(countSETypeError) + " records have standard error values that are not valid numbers.";
				}
				msg.message = strRecord + "";
				msg.type = "error";
				validationMsg.messages.add(msg);
			}
			if(countSEError > 0) {
				ValidationMessage.Message msg = new ValidationMessage.Message();
				String strRecord = "";
				if(countValueError == 1) {
					strRecord = String.valueOf(countSEError) + " record has";
				}
				else {
					strRecord = String.valueOf(countSEError) + " records have";
				}
				msg.message = strRecord + " standard error values below zero.";
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

			if(lstUndefinedRaces.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Ethnicity values are not defined: " + String.join(",", lstUndefinedRaces) + ".";
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

			if(lstUndefinedYears.size()>0) {
				validationMsg.success = false;
				ValidationMessage.Message msg = new ValidationMessage.Message();
				msg.message = "The following Year values are invalid: " + String.join(",", lstUndefinedYears) + ".";
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
			

			//Create the incidence record
			incRecord = DSL.using(JooqUtil.getJooqConfiguration())
			.insertInto(INCIDENCE_DATASET
					, INCIDENCE_DATASET.NAME
					, INCIDENCE_DATASET.GRID_DEFINITION_ID
					, INCIDENCE_DATASET.USER_ID
					, INCIDENCE_DATASET.SHARE_SCOPE
					, INCIDENCE_DATASET.FILENAME
					, INCIDENCE_DATASET.UPLOAD_DATE
					)
			.values(incidenceName,  gridId, userId, Constants.SHARING_NONE, filename, uploadDate)
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
		

		int rowCount = 0;
		while ((record = csvReader.readNext()) != null) {
			//use the hashmaps created from incidenceUtil to get the id of each column metric
				String endpointGroupName = record[endpointGroupIdx].toLowerCase();
				int endpointGroupId = endpointGroupIdLookup.get(endpointGroupName);
				int endpointId = endpointIdLookup.get(endpointGroupName).get(record[endpointIdx].toLowerCase());
				int row = Integer.valueOf(record[rowIdx]);
				int column = Integer.valueOf(record[columnIdx]);

				String raceName = record[raceIdx].toLowerCase();
				if (raceName.equals("")){
					raceName = "all";
				}
				int raceId = raceIdLookup.get(raceName);

				String genderName = record[genderIdx].toLowerCase();
				if (genderName.equals("")){
					genderName = "all";
				}
				int genderId = genderIdLookup.get(genderName);

				String ethnicityName = record[ethnicityIdx].toLowerCase();
				if (ethnicityName.equals("")){
					ethnicityName = "all";
				}
				int ethnicityId = ethnicityIdLookup.get(ethnicityName);
				int year = Integer.valueOf(record[yearIdx]);
				short startAge = Short.valueOf(record[startAgeIdx]);
				short endAge = Short.valueOf(record[endAgeIdx]);
				boolean prevalence = "prevalence".equalsIgnoreCase(record[typeIdx]);
				Double standardError = null;
				if(!record[standardErrorIdx].equals("")){
					standardError = Double.valueOf(record[standardErrorIdx]);
				}

				//check if there is a matching incidence_entry_id for the current row's metrics
				String entryQuery = String.format("incidence_dataset_id=%d and grid_definition_id=%d and endpoint_group_id=%d and endpoint_id=%d and race_id=%d and gender_id=%d and year=%d and start_age=%d and end_age=%d and type='%s' and ethnicity_id=%d",
				incidenceDatasetId,
				gridId,
				endpointGroupId,
				endpointId,
				raceId,
				genderId,
				year,
				startAge,
				endAge,
				prevalence,
				ethnicityId
				);

				int incidenceEntryId = -999;
				//if the metric combo isn't in hashmap, add to database and update hashmap
				if (!incidenceEntryIds.containsKey(entryQuery)){
					entryRecord = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							INCIDENCE_ENTRY, 
							INCIDENCE_ENTRY.INCIDENCE_DATASET_ID,
							INCIDENCE_ENTRY.ENDPOINT_GROUP_ID,
							INCIDENCE_ENTRY.ENDPOINT_ID,
							INCIDENCE_ENTRY.RACE_ID,
							INCIDENCE_ENTRY.GENDER_ID,
							INCIDENCE_ENTRY.YEAR,
							INCIDENCE_ENTRY.START_AGE,
							INCIDENCE_ENTRY.END_AGE,
							INCIDENCE_ENTRY.PREVALENCE,
							INCIDENCE_ENTRY.ETHNICITY_ID,
							INCIDENCE_ENTRY.TIMEFRAME,
							INCIDENCE_ENTRY.UNITS,
							INCIDENCE_ENTRY.DISTRIBUTION,
							INCIDENCE_ENTRY.STANDARD_ERROR
					)
					.values(				
						incidenceDatasetId, 
						endpointGroupId,
						endpointId,
						raceId,
						genderId,
						Integer.valueOf(record[yearIdx]),
						Short.valueOf(record[startAgeIdx]),
						Short.valueOf(record[endAgeIdx]),
						"prevalence".equalsIgnoreCase(record[typeIdx]),
						ethnicityId,
						record[timeFrameIdx].toLowerCase(),
						record[unitsIdx].toLowerCase(),
						record[distributionIdx].toLowerCase(),
						standardError

					)
					.returning(INCIDENCE_ENTRY.ID,INCIDENCE_ENTRY.INCIDENCE_DATASET_ID)
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

				rowCount++;

				//insert every 250000 rows and clear heap space
				if(rowCount == 250000) {
					batch2.execute();
					batch2.close();
					batch2 = DSL.using(JooqUtil.getJooqConfiguration())
					.insertInto(
							INCIDENCE_VALUE,
							INCIDENCE_VALUE.INCIDENCE_ENTRY_ID,
							INCIDENCE_VALUE.GRID_CELL_ID,
							INCIDENCE_VALUE.GRID_COL,
							INCIDENCE_VALUE.GRID_ROW,
							INCIDENCE_VALUE.VALUE
					);
					rowCount = 0;
				}
				
			}
			
			batch2.execute();

			
		
		} catch (Exception e) {
			log.error("Error importing incidence file", e);
			response.type("application/json");
			validationMsg.success=false;
			validationMsg.messages.add(new ValidationMessage.Message("error","Error occurred during import of incidence file."));
			deleteIncidenceDataset(incidenceDatasetId, userProfile);
			return transformValMsgToJSON(validationMsg);
		}
		
		response.type("application/json");
		validationMsg.success = true;
		return transformValMsgToJSON(validationMsg); 
	}


	// This version of this method is used when an error occurs during incidence upload to clean up
	private static void deleteIncidenceDataset(Integer id, Optional<UserProfile> userProfile) {
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		try {
			//first, get the incidence entry ids that would be deleted and delete the rows from the incidence values table that have that entry id
			List<Integer> deletedIncidenceEntryIds = create.transactionResult(configuration -> {
				DSLContext context = DSL.using(configuration);

				Result<IncidenceValueRecord> result = context
					.deleteFrom(INCIDENCE_VALUE)
					.where(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID.in(
						context.select(INCIDENCE_ENTRY.ID)
							.from(INCIDENCE_ENTRY)
							.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(id))
					))
					.returning(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID)
					.fetch();
				return result.getValues(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID);
			});

			// Next, delete the entries using the collected incidence entry IDs
			if(deletedIncidenceEntryIds.size() == 0) {
				try {
					create.deleteFrom(INCIDENCE_ENTRY)
						.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(id))
						.execute();
				} catch (DataAccessException e) {
				}
			} else {
				try {
					create.deleteFrom(INCIDENCE_ENTRY)
						.where(INCIDENCE_ENTRY.ID.in(deletedIncidenceEntryIds))
						.execute();
				} catch (DataAccessException e) {
				}
			}

			// Finally, delete the dataset from the incidence datasets table
			create.deleteFrom(INCIDENCE_DATASET)
				.where(INCIDENCE_DATASET.ID.eq(id))
				.execute();
					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// We want to silently fail in this case. At least we tried to clean up.
		}
	}

	/**
	 * Deletes an incidence dataset from the database (incidence id is a request parameter).
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return
	 */
	public static Object deleteIncidenceDataset(Request request, Response response, Optional<UserProfile> userProfile) {
		
		Integer id;
		try {
			id = Integer.valueOf(request.params("id"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return CoreApi.getErrorResponseInvalidId(request, response);
		} 
		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		
		IncidenceDatasetRecord datasetResult = create.selectFrom(INCIDENCE_DATASET).where(INCIDENCE_DATASET.ID.eq(id)).fetchAny();
		if(datasetResult == null) {
			return CoreApi.getErrorResponseNotFound(request, response);
		}
		
		//Nobody can delete shared layers
		//All users can delete their own layers
		//Admins can delete any non-shared layers
		if(datasetResult.getShareScope() == Constants.SHARING_ALL || !(datasetResult.getUserId().equalsIgnoreCase(userProfile.get().getId()) || CoreApi.isAdmin(userProfile)) )  {
			return CoreApi.getErrorResponseForbidden(request, response);
		}

		//first, get the incidence entry ids that would be deleted and delete the rows from the incidence values table that have that entry id
		List<Integer> deletedIncidenceEntryIds = create.transactionResult(configuration -> {
			DSLContext context = DSL.using(configuration);

			Result<IncidenceValueRecord> result = context
				.deleteFrom(INCIDENCE_VALUE)
				.where(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID.in(
					context.select(INCIDENCE_ENTRY.ID)
						.from(INCIDENCE_ENTRY)
						.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(id))
				))
				.returning(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID)
				.fetch();

			return result.getValues(INCIDENCE_VALUE.INCIDENCE_ENTRY_ID);
		});

		// Next, delete the entries using the collected incidence entry IDs
		if(deletedIncidenceEntryIds.size() == 0) {
			try {
				create.deleteFrom(INCIDENCE_ENTRY)
					.where(INCIDENCE_ENTRY.INCIDENCE_DATASET_ID.eq(id))
					.execute();
			} catch (DataAccessException e) {
			}
		} else {
			try {
				create.deleteFrom(INCIDENCE_ENTRY)
					.where(INCIDENCE_ENTRY.ID.in(deletedIncidenceEntryIds))
					.execute();
			} catch (DataAccessException e) {
			}
		}

		// Finally, delete the dataset from the incidence datasets table
		int numDeletedDatasets = create.deleteFrom(INCIDENCE_DATASET)
			.where(INCIDENCE_DATASET.ID.eq(id))
			.execute();
			
		//if no datasets were deleted, return an error
		if(numDeletedDatasets == 0) {
			return CoreApi.getErrorResponse(request, response, 400, "Unknown error");
		} else {
			response.status(204);
			return response;
		}
	} 
	

	/**
	 * 
	 * @param filterValue
	 * @return a condition object representing an incidence cell filter condition.
	 */
	private static Condition buildIncidenceCellsFilterCondition(String filterValue) {

		Condition filterCondition = DSL.trueCondition();
		Condition searchCondition = DSL.falseCondition();

		Short filterValueAsShort = DataConversionUtil.getFilterValueAsShort(filterValue);
		Integer filterValueAsInteger = DataConversionUtil.getFilterValueAsInteger(filterValue);
		Long filterValueAsLong = DataConversionUtil.getFilterValueAsLong(filterValue);
		Double filterValueAsDouble = DataConversionUtil.getFilterValueAsDouble(filterValue);
		BigDecimal filterValueAsBigDecimal = DataConversionUtil.getFilterValueAsBigDecimal(filterValue);
		Date filterValueAsDate = DataConversionUtil.getFilterValueAsDate(filterValue, "MM/dd/yyyy");
		

		searchCondition = 
				searchCondition.or(ENDPOINT_GROUP.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(ETHNICITY.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(ENDPOINT.NAME
						.containsIgnoreCase(filterValue));

		searchCondition = 
				searchCondition.or(ENDPOINT_GROUP.NAME
						.containsIgnoreCase(filterValue));			
						
		searchCondition = 
				searchCondition.or(GENDER.NAME
						.containsIgnoreCase(filterValue));			
		
		searchCondition = 
				searchCondition.or(INCIDENCE_ENTRY.UNITS
						.containsIgnoreCase(filterValue));		
		searchCondition = 
				searchCondition.or(INCIDENCE_ENTRY.TIMEFRAME
						.containsIgnoreCase(filterValue));		
		
		searchCondition = 
				searchCondition.or(INCIDENCE_ENTRY.DISTRIBUTION
						.containsIgnoreCase(filterValue));		




		if (null != filterValueAsInteger) {

			searchCondition = 
					searchCondition.or(INCIDENCE_VALUE.GRID_COL
							.eq(filterValueAsInteger));

			searchCondition = 
					searchCondition.or(INCIDENCE_VALUE.GRID_ROW
							.eq(filterValueAsInteger));
		}

		if (null != filterValueAsShort) {
			searchCondition = 
					searchCondition.or(INCIDENCE_ENTRY.START_AGE
							.eq(filterValueAsShort));

			searchCondition = 
					searchCondition.or(INCIDENCE_ENTRY.START_AGE
							.eq(filterValueAsShort));
		}

		if (null != filterValueAsDouble) {
			searchCondition = 
					searchCondition.or(INCIDENCE_VALUE.VALUE
							.eq(filterValueAsDouble));		

		searchCondition = 
					searchCondition.or(INCIDENCE_ENTRY.STANDARD_ERROR
							.eq(filterValueAsDouble));		
		}
		
		filterCondition = filterCondition.and(searchCondition);

		return filterCondition;
	}


	/**
	 * Sets the sort order of the incidence cells.
	 * @param sortBy
	 * @param descending
	 * @param orderFields
	 */
	private static void setIncidenceCellsSortOrder(
			String sortBy, Boolean descending, List<OrderField<?>> orderFields) {
		
		if (null != sortBy) {
			
			SortOrder sortDirection = SortOrder.ASC;
			Field<?> sortField = null;
			
			sortDirection = descending ? SortOrder.DESC : SortOrder.ASC;
			
			switch (sortBy) {
			case "grid_col":
				sortField = DSL.field(sortBy, Integer.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "grid_row":
				sortField = DSL.field(sortBy, Integer.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "gender":
				sortField = DSL.field(sortBy, String.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "race":
				sortField = DSL.field(sortBy, String.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "ethnicity":
				sortField = DSL.field(sortBy, Double.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			case "type":
				sortField = DSL.field(sortBy, Double.class.getName());
				orderFields.add(sortField.sort(sortDirection));
				break;

			default:
				//System.out.println("... in default...");
				orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
				orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
				break;
			}
			
		} else {
			orderFields.add(DSL.field("grid_col", Integer.class.getName()).sort(SortOrder.ASC));	
			orderFields.add(DSL.field("grid_row", Integer.class.getName()).sort(SortOrder.ASC));	
		}
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
		/**
	 * 
	 * @param layerName
	 * @return a csv file name for a given dataset name.
	 */
	public static String createFilename(String datasetName) {
		// Currently allowing periods so we don't break extensions. Need to improve this.
		return datasetName.replaceAll("[^A-Za-z0-9._-]+", "");
	}

}
