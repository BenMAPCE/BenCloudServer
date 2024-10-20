/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.SeasonalMetricSeason;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SeasonalMetricSeasonRecord extends UpdatableRecordImpl<SeasonalMetricSeasonRecord> implements Record6<Integer, Integer, Short, Short, Short, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.seasonal_metric_season.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.seasonal_metric_season.seasonal_metric_id</code>.
     */
    public void setSeasonalMetricId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.seasonal_metric_id</code>.
     */
    public Integer getSeasonalMetricId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>data.seasonal_metric_season.start_day</code>.
     */
    public void setStartDay(Short value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.start_day</code>.
     */
    public Short getStartDay() {
        return (Short) get(2);
    }

    /**
     * Setter for <code>data.seasonal_metric_season.end_day</code>.
     */
    public void setEndDay(Short value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.end_day</code>.
     */
    public Short getEndDay() {
        return (Short) get(3);
    }

    /**
     * Setter for <code>data.seasonal_metric_season.seasonal_metric_type</code>.
     */
    public void setSeasonalMetricType(Short value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.seasonal_metric_type</code>.
     */
    public Short getSeasonalMetricType() {
        return (Short) get(4);
    }

    /**
     * Setter for <code>data.seasonal_metric_season.metric_function</code>.
     */
    public void setMetricFunction(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>data.seasonal_metric_season.metric_function</code>.
     */
    public String getMetricFunction() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Short, Short, Short, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, Integer, Short, Short, Short, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.ID;
    }

    @Override
    public Field<Integer> field2() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.SEASONAL_METRIC_ID;
    }

    @Override
    public Field<Short> field3() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.START_DAY;
    }

    @Override
    public Field<Short> field4() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.END_DAY;
    }

    @Override
    public Field<Short> field5() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.SEASONAL_METRIC_TYPE;
    }

    @Override
    public Field<String> field6() {
        return SeasonalMetricSeason.SEASONAL_METRIC_SEASON.METRIC_FUNCTION;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getSeasonalMetricId();
    }

    @Override
    public Short component3() {
        return getStartDay();
    }

    @Override
    public Short component4() {
        return getEndDay();
    }

    @Override
    public Short component5() {
        return getSeasonalMetricType();
    }

    @Override
    public String component6() {
        return getMetricFunction();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getSeasonalMetricId();
    }

    @Override
    public Short value3() {
        return getStartDay();
    }

    @Override
    public Short value4() {
        return getEndDay();
    }

    @Override
    public Short value5() {
        return getSeasonalMetricType();
    }

    @Override
    public String value6() {
        return getMetricFunction();
    }

    @Override
    public SeasonalMetricSeasonRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord value2(Integer value) {
        setSeasonalMetricId(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord value3(Short value) {
        setStartDay(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord value4(Short value) {
        setEndDay(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord value5(Short value) {
        setSeasonalMetricType(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord value6(String value) {
        setMetricFunction(value);
        return this;
    }

    @Override
    public SeasonalMetricSeasonRecord values(Integer value1, Integer value2, Short value3, Short value4, Short value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SeasonalMetricSeasonRecord
     */
    public SeasonalMetricSeasonRecord() {
        super(SeasonalMetricSeason.SEASONAL_METRIC_SEASON);
    }

    /**
     * Create a detached, initialised SeasonalMetricSeasonRecord
     */
    public SeasonalMetricSeasonRecord(Integer id, Integer seasonalMetricId, Short startDay, Short endDay, Short seasonalMetricType, String metricFunction) {
        super(SeasonalMetricSeason.SEASONAL_METRIC_SEASON);

        setId(id);
        setSeasonalMetricId(seasonalMetricId);
        setStartDay(startDay);
        setEndDay(endDay);
        setSeasonalMetricType(seasonalMetricType);
        setMetricFunction(metricFunction);
    }
}
