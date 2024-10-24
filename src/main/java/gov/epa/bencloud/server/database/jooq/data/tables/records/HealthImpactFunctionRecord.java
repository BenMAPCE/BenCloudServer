/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.HealthImpactFunction;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class HealthImpactFunctionRecord extends UpdatableRecordImpl<HealthImpactFunctionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.health_impact_function.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.health_impact_function.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for
     * <code>data.health_impact_function.health_impact_function_dataset_id</code>.
     */
    public void setHealthImpactFunctionDatasetId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for
     * <code>data.health_impact_function.health_impact_function_dataset_id</code>.
     */
    public Integer getHealthImpactFunctionDatasetId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>data.health_impact_function.endpoint_group_id</code>.
     */
    public void setEndpointGroupId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.health_impact_function.endpoint_group_id</code>.
     */
    public Integer getEndpointGroupId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>data.health_impact_function.endpoint_id</code>.
     */
    public void setEndpointId(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.health_impact_function.endpoint_id</code>.
     */
    public Integer getEndpointId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>data.health_impact_function.pollutant_id</code>.
     */
    public void setPollutantId(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.health_impact_function.pollutant_id</code>.
     */
    public Integer getPollutantId() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>data.health_impact_function.metric_id</code>.
     */
    public void setMetricId(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>data.health_impact_function.metric_id</code>.
     */
    public Integer getMetricId() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>data.health_impact_function.seasonal_metric_id</code>.
     */
    public void setSeasonalMetricId(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>data.health_impact_function.seasonal_metric_id</code>.
     */
    public Integer getSeasonalMetricId() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>data.health_impact_function.metric_statistic</code>.
     */
    public void setMetricStatistic(Integer value) {
        set(7, value);
    }

    /**
     * Getter for <code>data.health_impact_function.metric_statistic</code>.
     */
    public Integer getMetricStatistic() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>data.health_impact_function.author</code>.
     */
    public void setAuthor(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>data.health_impact_function.author</code>.
     */
    public String getAuthor() {
        return (String) get(8);
    }

    /**
     * Setter for <code>data.health_impact_function.function_year</code>.
     */
    public void setFunctionYear(Integer value) {
        set(9, value);
    }

    /**
     * Getter for <code>data.health_impact_function.function_year</code>.
     */
    public Integer getFunctionYear() {
        return (Integer) get(9);
    }

    /**
     * Setter for <code>data.health_impact_function.location</code>.
     */
    public void setLocation(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>data.health_impact_function.location</code>.
     */
    public String getLocation() {
        return (String) get(10);
    }

    /**
     * Setter for <code>data.health_impact_function.other_pollutants</code>.
     */
    public void setOtherPollutants(String value) {
        set(11, value);
    }

    /**
     * Getter for <code>data.health_impact_function.other_pollutants</code>.
     */
    public String getOtherPollutants() {
        return (String) get(11);
    }

    /**
     * Setter for <code>data.health_impact_function.qualifier</code>.
     */
    public void setQualifier_(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>data.health_impact_function.qualifier</code>.
     */
    public String getQualifier_() {
        return (String) get(12);
    }

    /**
     * Setter for <code>data.health_impact_function.reference</code>.
     */
    public void setReference(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>data.health_impact_function.reference</code>.
     */
    public String getReference() {
        return (String) get(13);
    }

    /**
     * Setter for <code>data.health_impact_function.start_age</code>.
     */
    public void setStartAge(Integer value) {
        set(14, value);
    }

    /**
     * Getter for <code>data.health_impact_function.start_age</code>.
     */
    public Integer getStartAge() {
        return (Integer) get(14);
    }

    /**
     * Setter for <code>data.health_impact_function.end_age</code>.
     */
    public void setEndAge(Integer value) {
        set(15, value);
    }

    /**
     * Getter for <code>data.health_impact_function.end_age</code>.
     */
    public Integer getEndAge() {
        return (Integer) get(15);
    }

    /**
     * Setter for <code>data.health_impact_function.function_text</code>.
     */
    public void setFunctionText(String value) {
        set(16, value);
    }

    /**
     * Getter for <code>data.health_impact_function.function_text</code>.
     */
    public String getFunctionText() {
        return (String) get(16);
    }

    /**
     * Setter for <code>data.health_impact_function.incidence_dataset_id</code>.
     */
    public void setIncidenceDatasetId(Integer value) {
        set(17, value);
    }

    /**
     * Getter for <code>data.health_impact_function.incidence_dataset_id</code>.
     */
    public Integer getIncidenceDatasetId() {
        return (Integer) get(17);
    }

    /**
     * Setter for
     * <code>data.health_impact_function.prevalence_dataset_id</code>.
     */
    public void setPrevalenceDatasetId(Integer value) {
        set(18, value);
    }

    /**
     * Getter for
     * <code>data.health_impact_function.prevalence_dataset_id</code>.
     */
    public Integer getPrevalenceDatasetId() {
        return (Integer) get(18);
    }

    /**
     * Setter for <code>data.health_impact_function.variable_dataset_id</code>.
     */
    public void setVariableDatasetId(Integer value) {
        set(19, value);
    }

    /**
     * Getter for <code>data.health_impact_function.variable_dataset_id</code>.
     */
    public Integer getVariableDatasetId() {
        return (Integer) get(19);
    }

    /**
     * Setter for <code>data.health_impact_function.beta</code>.
     */
    public void setBeta(Double value) {
        set(20, value);
    }

    /**
     * Getter for <code>data.health_impact_function.beta</code>.
     */
    public Double getBeta() {
        return (Double) get(20);
    }

    /**
     * Setter for <code>data.health_impact_function.dist_beta</code>.
     */
    public void setDistBeta(String value) {
        set(21, value);
    }

    /**
     * Getter for <code>data.health_impact_function.dist_beta</code>.
     */
    public String getDistBeta() {
        return (String) get(21);
    }

    /**
     * Setter for <code>data.health_impact_function.p1_beta</code>.
     */
    public void setP1Beta(Double value) {
        set(22, value);
    }

    /**
     * Getter for <code>data.health_impact_function.p1_beta</code>.
     */
    public Double getP1Beta() {
        return (Double) get(22);
    }

    /**
     * Setter for <code>data.health_impact_function.p2_beta</code>.
     */
    public void setP2Beta(Double value) {
        set(23, value);
    }

    /**
     * Getter for <code>data.health_impact_function.p2_beta</code>.
     */
    public Double getP2Beta() {
        return (Double) get(23);
    }

    /**
     * Setter for <code>data.health_impact_function.val_a</code>.
     */
    public void setValA(Double value) {
        set(24, value);
    }

    /**
     * Getter for <code>data.health_impact_function.val_a</code>.
     */
    public Double getValA() {
        return (Double) get(24);
    }

    /**
     * Setter for <code>data.health_impact_function.name_a</code>.
     */
    public void setNameA(String value) {
        set(25, value);
    }

    /**
     * Getter for <code>data.health_impact_function.name_a</code>.
     */
    public String getNameA() {
        return (String) get(25);
    }

    /**
     * Setter for <code>data.health_impact_function.val_b</code>.
     */
    public void setValB(Double value) {
        set(26, value);
    }

    /**
     * Getter for <code>data.health_impact_function.val_b</code>.
     */
    public Double getValB() {
        return (Double) get(26);
    }

    /**
     * Setter for <code>data.health_impact_function.name_b</code>.
     */
    public void setNameB(String value) {
        set(27, value);
    }

    /**
     * Getter for <code>data.health_impact_function.name_b</code>.
     */
    public String getNameB() {
        return (String) get(27);
    }

    /**
     * Setter for <code>data.health_impact_function.val_c</code>.
     */
    public void setValC(Double value) {
        set(28, value);
    }

    /**
     * Getter for <code>data.health_impact_function.val_c</code>.
     */
    public Double getValC() {
        return (Double) get(28);
    }

    /**
     * Setter for <code>data.health_impact_function.name_c</code>.
     */
    public void setNameC(String value) {
        set(29, value);
    }

    /**
     * Getter for <code>data.health_impact_function.name_c</code>.
     */
    public String getNameC() {
        return (String) get(29);
    }

    /**
     * Setter for
     * <code>data.health_impact_function.baseline_function_text</code>.
     */
    public void setBaselineFunctionText(String value) {
        set(30, value);
    }

    /**
     * Getter for
     * <code>data.health_impact_function.baseline_function_text</code>.
     */
    public String getBaselineFunctionText() {
        return (String) get(30);
    }

    /**
     * Setter for <code>data.health_impact_function.race_id</code>.
     */
    public void setRaceId(Integer value) {
        set(31, value);
    }

    /**
     * Getter for <code>data.health_impact_function.race_id</code>.
     */
    public Integer getRaceId() {
        return (Integer) get(31);
    }

    /**
     * Setter for <code>data.health_impact_function.gender_id</code>.
     */
    public void setGenderId(Integer value) {
        set(32, value);
    }

    /**
     * Getter for <code>data.health_impact_function.gender_id</code>.
     */
    public Integer getGenderId() {
        return (Integer) get(32);
    }

    /**
     * Setter for <code>data.health_impact_function.ethnicity_id</code>.
     */
    public void setEthnicityId(Integer value) {
        set(33, value);
    }

    /**
     * Getter for <code>data.health_impact_function.ethnicity_id</code>.
     */
    public Integer getEthnicityId() {
        return (Integer) get(33);
    }

    /**
     * Setter for <code>data.health_impact_function.start_day</code>.
     */
    public void setStartDay(Integer value) {
        set(34, value);
    }

    /**
     * Getter for <code>data.health_impact_function.start_day</code>.
     */
    public Integer getStartDay() {
        return (Integer) get(34);
    }

    /**
     * Setter for <code>data.health_impact_function.end_day</code>.
     */
    public void setEndDay(Integer value) {
        set(35, value);
    }

    /**
     * Getter for <code>data.health_impact_function.end_day</code>.
     */
    public Integer getEndDay() {
        return (Integer) get(35);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached HealthImpactFunctionRecord
     */
    public HealthImpactFunctionRecord() {
        super(HealthImpactFunction.HEALTH_IMPACT_FUNCTION);
    }

    /**
     * Create a detached, initialised HealthImpactFunctionRecord
     */
    public HealthImpactFunctionRecord(Integer id, Integer healthImpactFunctionDatasetId, Integer endpointGroupId, Integer endpointId, Integer pollutantId, Integer metricId, Integer seasonalMetricId, Integer metricStatistic, String author, Integer functionYear, String location, String otherPollutants, String qualifier, String reference, Integer startAge, Integer endAge, String functionText, Integer incidenceDatasetId, Integer prevalenceDatasetId, Integer variableDatasetId, Double beta, String distBeta, Double p1Beta, Double p2Beta, Double valA, String nameA, Double valB, String nameB, Double valC, String nameC, String baselineFunctionText, Integer raceId, Integer genderId, Integer ethnicityId, Integer startDay, Integer endDay) {
        super(HealthImpactFunction.HEALTH_IMPACT_FUNCTION);

        setId(id);
        setHealthImpactFunctionDatasetId(healthImpactFunctionDatasetId);
        setEndpointGroupId(endpointGroupId);
        setEndpointId(endpointId);
        setPollutantId(pollutantId);
        setMetricId(metricId);
        setSeasonalMetricId(seasonalMetricId);
        setMetricStatistic(metricStatistic);
        setAuthor(author);
        setFunctionYear(functionYear);
        setLocation(location);
        setOtherPollutants(otherPollutants);
        setQualifier_(qualifier);
        setReference(reference);
        setStartAge(startAge);
        setEndAge(endAge);
        setFunctionText(functionText);
        setIncidenceDatasetId(incidenceDatasetId);
        setPrevalenceDatasetId(prevalenceDatasetId);
        setVariableDatasetId(variableDatasetId);
        setBeta(beta);
        setDistBeta(distBeta);
        setP1Beta(p1Beta);
        setP2Beta(p2Beta);
        setValA(valA);
        setNameA(nameA);
        setValB(valB);
        setNameB(nameB);
        setValC(valC);
        setNameC(nameC);
        setBaselineFunctionText(baselineFunctionText);
        setRaceId(raceId);
        setGenderId(genderId);
        setEthnicityId(ethnicityId);
        setStartDay(startDay);
        setEndDay(endDay);
    }
}
