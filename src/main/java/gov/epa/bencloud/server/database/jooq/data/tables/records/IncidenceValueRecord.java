/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.IncidenceValue;

import org.jooq.Field;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class IncidenceValueRecord extends TableRecordImpl<IncidenceValueRecord> implements Record5<Integer, Long, Integer, Integer, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.incidence_value.incidence_entry_id</code>.
     */
    public void setIncidenceEntryId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.incidence_value.incidence_entry_id</code>.
     */
    public Integer getIncidenceEntryId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.incidence_value.grid_cell_id</code>.
     */
    public void setGridCellId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.incidence_value.grid_cell_id</code>.
     */
    public Long getGridCellId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>data.incidence_value.grid_col</code>.
     */
    public void setGridCol(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.incidence_value.grid_col</code>.
     */
    public Integer getGridCol() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>data.incidence_value.grid_row</code>.
     */
    public void setGridRow(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.incidence_value.grid_row</code>.
     */
    public Integer getGridRow() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>data.incidence_value.value</code>.
     */
    public void setValue(Double value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.incidence_value.value</code>.
     */
    public Double getValue() {
        return (Double) get(4);
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Long, Integer, Integer, Double> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, Long, Integer, Integer, Double> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return IncidenceValue.INCIDENCE_VALUE.INCIDENCE_ENTRY_ID;
    }

    @Override
    public Field<Long> field2() {
        return IncidenceValue.INCIDENCE_VALUE.GRID_CELL_ID;
    }

    @Override
    public Field<Integer> field3() {
        return IncidenceValue.INCIDENCE_VALUE.GRID_COL;
    }

    @Override
    public Field<Integer> field4() {
        return IncidenceValue.INCIDENCE_VALUE.GRID_ROW;
    }

    @Override
    public Field<Double> field5() {
        return IncidenceValue.INCIDENCE_VALUE.VALUE;
    }

    @Override
    public Integer component1() {
        return getIncidenceEntryId();
    }

    @Override
    public Long component2() {
        return getGridCellId();
    }

    @Override
    public Integer component3() {
        return getGridCol();
    }

    @Override
    public Integer component4() {
        return getGridRow();
    }

    @Override
    public Double component5() {
        return getValue();
    }

    @Override
    public Integer value1() {
        return getIncidenceEntryId();
    }

    @Override
    public Long value2() {
        return getGridCellId();
    }

    @Override
    public Integer value3() {
        return getGridCol();
    }

    @Override
    public Integer value4() {
        return getGridRow();
    }

    @Override
    public Double value5() {
        return getValue();
    }

    @Override
    public IncidenceValueRecord value1(Integer value) {
        setIncidenceEntryId(value);
        return this;
    }

    @Override
    public IncidenceValueRecord value2(Long value) {
        setGridCellId(value);
        return this;
    }

    @Override
    public IncidenceValueRecord value3(Integer value) {
        setGridCol(value);
        return this;
    }

    @Override
    public IncidenceValueRecord value4(Integer value) {
        setGridRow(value);
        return this;
    }

    @Override
    public IncidenceValueRecord value5(Double value) {
        setValue(value);
        return this;
    }

    @Override
    public IncidenceValueRecord values(Integer value1, Long value2, Integer value3, Integer value4, Double value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached IncidenceValueRecord
     */
    public IncidenceValueRecord() {
        super(IncidenceValue.INCIDENCE_VALUE);
    }

    /**
     * Create a detached, initialised IncidenceValueRecord
     */
    public IncidenceValueRecord(Integer incidenceEntryId, Long gridCellId, Integer gridCol, Integer gridRow, Double value) {
        super(IncidenceValue.INCIDENCE_VALUE);

        setIncidenceEntryId(incidenceEntryId);
        setGridCellId(gridCellId);
        setGridCol(gridCol);
        setGridRow(gridRow);
        setValue(value);
    }
}
