/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.tables.records.VariableValueRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VariableValue extends TableImpl<VariableValueRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.variable_value</code>
     */
    public static final VariableValue VARIABLE_VALUE = new VariableValue();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<VariableValueRecord> getRecordType() {
        return VariableValueRecord.class;
    }

    /**
     * The column <code>data.variable_value.variable_entry_id</code>.
     */
    public final TableField<VariableValueRecord, Integer> VARIABLE_ENTRY_ID = createField(DSL.name("variable_entry_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.variable_value.grid_col</code>.
     */
    public final TableField<VariableValueRecord, Integer> GRID_COL = createField(DSL.name("grid_col"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.variable_value.grid_row</code>.
     */
    public final TableField<VariableValueRecord, Integer> GRID_ROW = createField(DSL.name("grid_row"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.variable_value.value</code>.
     */
    public final TableField<VariableValueRecord, Double> VALUE = createField(DSL.name("value"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>data.variable_value.grid_cell_id</code>.
     */
    public final TableField<VariableValueRecord, Long> GRID_CELL_ID = createField(DSL.name("grid_cell_id"), SQLDataType.BIGINT, this, "");

    private VariableValue(Name alias, Table<VariableValueRecord> aliased) {
        this(alias, aliased, null);
    }

    private VariableValue(Name alias, Table<VariableValueRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.variable_value</code> table reference
     */
    public VariableValue(String alias) {
        this(DSL.name(alias), VARIABLE_VALUE);
    }

    /**
     * Create an aliased <code>data.variable_value</code> table reference
     */
    public VariableValue(Name alias) {
        this(alias, VARIABLE_VALUE);
    }

    /**
     * Create a <code>data.variable_value</code> table reference
     */
    public VariableValue() {
        this(DSL.name("variable_value"), null);
    }

    public <O extends Record> VariableValue(Table<O> child, ForeignKey<O, VariableValueRecord> key) {
        super(child, key, VARIABLE_VALUE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Data.DATA;
    }

    @Override
    public VariableValue as(String alias) {
        return new VariableValue(DSL.name(alias), this);
    }

    @Override
    public VariableValue as(Name alias) {
        return new VariableValue(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public VariableValue rename(String name) {
        return new VariableValue(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public VariableValue rename(Name name) {
        return new VariableValue(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Integer, Integer, Double, Long> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
