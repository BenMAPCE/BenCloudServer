/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.PopConfigEthnicity;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PopConfigEthnicityRecord extends TableRecordImpl<PopConfigEthnicityRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.pop_config_ethnicity.pop_config_id</code>.
     */
    public void setPopConfigId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.pop_config_ethnicity.pop_config_id</code>.
     */
    public Integer getPopConfigId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.pop_config_ethnicity.ethnicity_id</code>.
     */
    public void setEthnicityId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.pop_config_ethnicity.ethnicity_id</code>.
     */
    public Integer getEthnicityId() {
        return (Integer) get(1);
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, Integer> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return PopConfigEthnicity.POP_CONFIG_ETHNICITY.POP_CONFIG_ID;
    }

    @Override
    public Field<Integer> field2() {
        return PopConfigEthnicity.POP_CONFIG_ETHNICITY.ETHNICITY_ID;
    }

    @Override
    public Integer component1() {
        return getPopConfigId();
    }

    @Override
    public Integer component2() {
        return getEthnicityId();
    }

    @Override
    public Integer value1() {
        return getPopConfigId();
    }

    @Override
    public Integer value2() {
        return getEthnicityId();
    }

    @Override
    public PopConfigEthnicityRecord value1(Integer value) {
        setPopConfigId(value);
        return this;
    }

    @Override
    public PopConfigEthnicityRecord value2(Integer value) {
        setEthnicityId(value);
        return this;
    }

    @Override
    public PopConfigEthnicityRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PopConfigEthnicityRecord
     */
    public PopConfigEthnicityRecord() {
        super(PopConfigEthnicity.POP_CONFIG_ETHNICITY);
    }

    /**
     * Create a detached, initialised PopConfigEthnicityRecord
     */
    public PopConfigEthnicityRecord(Integer popConfigId, Integer ethnicityId) {
        super(PopConfigEthnicity.POP_CONFIG_ETHNICITY);

        setPopConfigId(popConfigId);
        setEthnicityId(ethnicityId);
    }
}
