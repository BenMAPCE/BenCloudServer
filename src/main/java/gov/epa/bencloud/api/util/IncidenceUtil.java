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
     * @param startAgeIdx
     * @param endAgeIdx
     * @param typeIdx
     * @param valueIdx
     * @return a string representing the missing columns
     */
    public static String validateModelColumnHeadings(int columnIdx, int rowIdx, int endpointIdx, int endpointGroupIdx, int raceIdx, int genderIdx, int ethnicityIdx, int startAgeIdx, int endAgeIdx, int typeIdx, int valueIdx) {
		StringBuilder b = new StringBuilder();
		if(endpointGroupIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Endpoint Group");
		}
		if(endpointIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Endpoint");
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
        // if(valuesIdx == -999) {
		// 	b.append((b.length()==0 ? "" : ", ") + "Timeframe");
		// }
        // if(valuesIdx == -999) {
		// 	b.append((b.length()==0 ? "" : ", ") + "Units");
		// }
        if(valueIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Value");
		}
        // if(valuesIdx == -999) {
		// 	b.append((b.length()==0 ? "" : ", ") + "Distribution");
		// }
        // if(valuesIdx == -999) {
		// 	b.append((b.length()==0 ? "" : ", ") + "Standard Error");
		// }

		return b.toString();
	}
    
    /**
     * 
     * @return a mapping of race names to Ids
     */
    public static Map<String, Integer> getRaceIdLookup() {
        Map<String, Integer> raceMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.field("name", String.class), DSL.field("id", Integer.class))
            .from(RACE)
            .fetchMap(DSL.field("name", String.class), DSL.field("id", Integer.class));
        return raceMetricMap;}
    
    /**
     * 
     * @return mapping of ethnicity names to Ids
     */
    public static Map<String, Integer> getEthnicityIdLookup() {
        Map<String, Integer> ethnicityMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.field("name", String.class), DSL.field("id", Integer.class))
            .from(ETHNICITY)
            .fetchMap(DSL.field("name", String.class), DSL.field("id", Integer.class));
        return ethnicityMetricMap;}
    
    /**
     * 
     * @return a mapping of gender names to Ids
     */
    public static Map<String, Integer> getGenderIdLookup() {
        Map<String, Integer> genderMetricMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.field("name", String.class), DSL.field("id", Integer.class))
            .from(GENDER)
            .fetchMap(DSL.field("name", String.class), DSL.field("id", Integer.class));
        return genderMetricMap;}


    /**
     * 
     * @param endpointGroupId
     * @return a mapping of endpoint names to endpoint ids for a given endpoint group Id
     */
    public static Map<String, Integer> getEndpointIdLookup(int endpointGroupId) {
        Map<String, Integer> endpointMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.field("name", String.class), DSL.field("id", Integer.class))
            .from(ENDPOINT)
            .where(DSL.field("endpoint_group_id").eq(endpointGroupId))
            .fetchMap(DSL.field("name", String.class), DSL.field("id", Integer.class));
        return endpointMap;}    
    
    
    /**
     * 
     * @return a mapping of endpoint group names to Ids
     */
    public static Map<String, Integer> getEndpointGroupIdLookup() {
        Map<String, Integer> endpointGroupMap = DSL.using(JooqUtil.getJooqConfiguration())
            .select(DSL.field("name", String.class), DSL.field("id", Integer.class))
            .from(ENDPOINT_GROUP)
            .fetchMap(DSL.field("name", String.class), DSL.field("id", Integer.class));
        return endpointGroupMap;}
    }