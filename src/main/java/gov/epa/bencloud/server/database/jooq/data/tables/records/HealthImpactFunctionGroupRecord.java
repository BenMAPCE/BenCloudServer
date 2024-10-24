/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.HealthImpactFunctionGroup;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class HealthImpactFunctionGroupRecord extends UpdatableRecordImpl<HealthImpactFunctionGroupRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.health_impact_function_group.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.health_impact_function_group.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.health_impact_function_group.name</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.health_impact_function_group.name</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>data.health_impact_function_group.help_text</code>.
     */
    public void setHelpText(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.health_impact_function_group.help_text</code>.
     */
    public String getHelpText() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return HealthImpactFunctionGroup.HEALTH_IMPACT_FUNCTION_GROUP.ID;
    }

    @Override
    public Field<String> field2() {
        return HealthImpactFunctionGroup.HEALTH_IMPACT_FUNCTION_GROUP.NAME;
    }

    @Override
    public Field<String> field3() {
        return HealthImpactFunctionGroup.HEALTH_IMPACT_FUNCTION_GROUP.HELP_TEXT;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public String component3() {
        return getHelpText();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public String value3() {
        return getHelpText();
    }

    @Override
    public HealthImpactFunctionGroupRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public HealthImpactFunctionGroupRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public HealthImpactFunctionGroupRecord value3(String value) {
        setHelpText(value);
        return this;
    }

    @Override
    public HealthImpactFunctionGroupRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached HealthImpactFunctionGroupRecord
     */
    public HealthImpactFunctionGroupRecord() {
        super(HealthImpactFunctionGroup.HEALTH_IMPACT_FUNCTION_GROUP);
    }

    /**
     * Create a detached, initialised HealthImpactFunctionGroupRecord
     */
    public HealthImpactFunctionGroupRecord(Integer id, String name, String helpText) {
        super(HealthImpactFunctionGroup.HEALTH_IMPACT_FUNCTION_GROUP);

        setId(id);
        setName(name);
        setHelpText(helpText);
    }
}
