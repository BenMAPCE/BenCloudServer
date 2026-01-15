package gov.epa.bencloud.api.util;

import java.util.Map;

import org.jooq.impl.DSL;

import gov.epa.bencloud.server.database.JooqUtil;
import static gov.epa.bencloud.server.database.jooq.data.Tables.*;


public class IncidenceUtil {

    /**
     * 
     * @param columnIdx
     * @param rowIdx
     * @param endpointIdx
     * @param endpointGroupIdx
     * @param raceIdx
     * @param genderIdx
     * @param ethnicityIdx
     * @param yearIdx
     * @param startAgeIdx
     * @param endAgeIdx
     * @param typeIdx
     * @param valueIdx
     * @return a string representing the missing columns
     */
    public static String validateModelColumnHeadings(int columnIdx, int rowIdx, int endpointIdx, int endpointGroupIdx, int raceIdx, int genderIdx, int ethnicityIdx, int yearIdx, int startAgeIdx, int endAgeIdx, int typeIdx, int timeframeIdx, int unitsIdx, int valueIdx, int distributionIdx, int standardErrorIdx) {
		StringBuilder b = new StringBuilder();
		if(endpointGroupIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Health Effect Group");
		}
		if(endpointIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Health Effect");
		}
		if(raceIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Race");
		}
		if(genderIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Gender");
		}
		if(ethnicityIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Ethnicity");
		}
        if(yearIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Year");
		}
        if(startAgeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Start Age");
		}
		if(endAgeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "End Age");
		}
        if(columnIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Column");
		}
        if(rowIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Row");
		}
        if(typeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Type");
		}
        if(timeframeIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Timeframe");
		}
        if(unitsIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Units");
		}
        if(valueIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Value");
		}
        if(distributionIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Distribution");
		}
        if(standardErrorIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Standard Error");
		}

		return b.toString();
	}
    
    /**
     * 
     * @return a mapping of race names to Ids
     */
    public static Map<String, Integer> getRaceIdLookup() {
        Map<String, Integer> raceMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(RACE.NAME), RACE.ID)
            .from(RACE)
            .fetchMap(DSL.lower(RACE.NAME), RACE.ID);
        return raceMetricMap;}
    
    /**
     * 
     * @return mapping of ethnicity names to Ids
     */
    public static Map<String, Integer> getEthnicityIdLookup() {
        Map<String, Integer> ethnicityMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(ETHNICITY.NAME), ETHNICITY.ID)
            .from(ETHNICITY)
            .fetchMap(DSL.lower(ETHNICITY.NAME), ETHNICITY.ID);
        return ethnicityMetricMap;}
    
    /**
     * 
     * @return a mapping of gender names to Ids
     */
    public static Map<String, Integer> getGenderIdLookup() {
        Map<String, Integer> genderMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(GENDER.NAME), GENDER.ID)
            .from(GENDER)
            .fetchMap(DSL.lower(GENDER.NAME), GENDER.ID);
        return genderMetricMap;}


    /**
     * 
     * @param endpointGroupId
     * @return a mapping of endpoint names to endpoint ids for a given endpoint group Id
     */
    public static Map<String, Integer> getEndpointIdLookup(short endpointGroupId) {
        Map<String, Integer> endpointMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(ENDPOINT.DISPLAY_NAME), ENDPOINT.ID)
            .from(ENDPOINT)
            .where(ENDPOINT.ENDPOINT_GROUP_ID.eq(endpointGroupId))
            .fetchMap(DSL.lower(ENDPOINT.DISPLAY_NAME), ENDPOINT.ID);
        return endpointMap;}    
    
    
    /**
     * 
     * @return a mapping of endpoint group names to Ids
     */
    public static Map<String, Integer> getEndpointGroupIdLookup() {
        Map<String, Integer> endpointGroupMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.lower(ENDPOINT_GROUP.NAME), ENDPOINT_GROUP.ID)
            .from(ENDPOINT_GROUP)
            .fetchMap(DSL.lower(ENDPOINT_GROUP.NAME), ENDPOINT_GROUP.ID);
        return endpointGroupMap;}
    }