/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.PopulationGrowthB2020T2020;

import org.jooq.Field;
import org.jooq.Record7;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PopulationGrowthB2020T2020Record extends UpdatableRecordImpl<PopulationGrowthB2020T2020Record> implements Record8<Short, Short, Integer, Integer, Integer, Integer, Integer, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.population_growth_b2020_t2020.base_pop_year</code>.
     */
    public void setBasePopYear(Short value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.base_pop_year</code>.
     */
    public Short getBasePopYear() {
        return (Short) get(0);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.pop_year</code>.
     */
    public void setPopYear(Short value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.pop_year</code>.
     */
    public Short getPopYear() {
        return (Short) get(1);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.race_id</code>.
     */
    public void setRaceId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.race_id</code>.
     */
    public Integer getRaceId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.gender_id</code>.
     */
    public void setGenderId(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.gender_id</code>.
     */
    public Integer getGenderId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.ethnicity_id</code>.
     */
    public void setEthnicityId(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.ethnicity_id</code>.
     */
    public Integer getEthnicityId() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.age_range_id</code>.
     */
    public void setAgeRangeId(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.age_range_id</code>.
     */
    public Integer getAgeRangeId() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.grid_cell_id</code>.
     */
    public void setGridCellId(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.grid_cell_id</code>.
     */
    public Integer getGridCellId() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>data.population_growth_b2020_t2020.growth_value</code>.
     */
    public void setGrowthValue(Double value) {
        set(7, value);
    }

    /**
     * Getter for <code>data.population_growth_b2020_t2020.growth_value</code>.
     */
    public Double getGrowthValue() {
        return (Double) get(7);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record7<Short, Short, Integer, Integer, Integer, Integer, Integer> key() {
        return (Record7) super.key();
    }

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row8<Short, Short, Integer, Integer, Integer, Integer, Integer, Double> fieldsRow() {
        return (Row8) super.fieldsRow();
    }

    @Override
    public Row8<Short, Short, Integer, Integer, Integer, Integer, Integer, Double> valuesRow() {
        return (Row8) super.valuesRow();
    }

    @Override
    public Field<Short> field1() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.BASE_POP_YEAR;
    }

    @Override
    public Field<Short> field2() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.POP_YEAR;
    }

    @Override
    public Field<Integer> field3() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.RACE_ID;
    }

    @Override
    public Field<Integer> field4() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.GENDER_ID;
    }

    @Override
    public Field<Integer> field5() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.ETHNICITY_ID;
    }

    @Override
    public Field<Integer> field6() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.AGE_RANGE_ID;
    }

    @Override
    public Field<Integer> field7() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.GRID_CELL_ID;
    }

    @Override
    public Field<Double> field8() {
        return PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020.GROWTH_VALUE;
    }

    @Override
    public Short component1() {
        return getBasePopYear();
    }

    @Override
    public Short component2() {
        return getPopYear();
    }

    @Override
    public Integer component3() {
        return getRaceId();
    }

    @Override
    public Integer component4() {
        return getGenderId();
    }

    @Override
    public Integer component5() {
        return getEthnicityId();
    }

    @Override
    public Integer component6() {
        return getAgeRangeId();
    }

    @Override
    public Integer component7() {
        return getGridCellId();
    }

    @Override
    public Double component8() {
        return getGrowthValue();
    }

    @Override
    public Short value1() {
        return getBasePopYear();
    }

    @Override
    public Short value2() {
        return getPopYear();
    }

    @Override
    public Integer value3() {
        return getRaceId();
    }

    @Override
    public Integer value4() {
        return getGenderId();
    }

    @Override
    public Integer value5() {
        return getEthnicityId();
    }

    @Override
    public Integer value6() {
        return getAgeRangeId();
    }

    @Override
    public Integer value7() {
        return getGridCellId();
    }

    @Override
    public Double value8() {
        return getGrowthValue();
    }

    @Override
    public PopulationGrowthB2020T2020Record value1(Short value) {
        setBasePopYear(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value2(Short value) {
        setPopYear(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value3(Integer value) {
        setRaceId(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value4(Integer value) {
        setGenderId(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value5(Integer value) {
        setEthnicityId(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value6(Integer value) {
        setAgeRangeId(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value7(Integer value) {
        setGridCellId(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record value8(Double value) {
        setGrowthValue(value);
        return this;
    }

    @Override
    public PopulationGrowthB2020T2020Record values(Short value1, Short value2, Integer value3, Integer value4, Integer value5, Integer value6, Integer value7, Double value8) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PopulationGrowthB2020T2020Record
     */
    public PopulationGrowthB2020T2020Record() {
        super(PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020);
    }

    /**
     * Create a detached, initialised PopulationGrowthB2020T2020Record
     */
    public PopulationGrowthB2020T2020Record(Short basePopYear, Short popYear, Integer raceId, Integer genderId, Integer ethnicityId, Integer ageRangeId, Integer gridCellId, Double growthValue) {
        super(PopulationGrowthB2020T2020.POPULATION_GROWTH_B2020_T2020);

        setBasePopYear(basePopYear);
        setPopYear(popYear);
        setRaceId(raceId);
        setGenderId(genderId);
        setEthnicityId(ethnicityId);
        setAgeRangeId(ageRangeId);
        setGridCellId(gridCellId);
        setGrowthValue(growthValue);
    }
}
