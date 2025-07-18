/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.Keys;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GridDefinitionRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.JSON;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row12;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class GridDefinition extends TableImpl<GridDefinitionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.grid_definition</code>
     */
    public static final GridDefinition GRID_DEFINITION = new GridDefinition();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<GridDefinitionRecord> getRecordType() {
        return GridDefinitionRecord.class;
    }

    /**
     * The column <code>data.grid_definition.id</code>.
     */
    public final TableField<GridDefinitionRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>data.grid_definition.name</code>.
     */
    public final TableField<GridDefinitionRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>data.grid_definition.col_count</code>.
     */
    public final TableField<GridDefinitionRecord, Integer> COL_COUNT = createField(DSL.name("col_count"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.grid_definition.row_count</code>.
     */
    public final TableField<GridDefinitionRecord, Integer> ROW_COUNT = createField(DSL.name("row_count"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.grid_definition.is_admin_layer</code>.
     */
    public final TableField<GridDefinitionRecord, String> IS_ADMIN_LAYER = createField(DSL.name("is_admin_layer"), SQLDataType.VARCHAR(1), this, "");

    /**
     * The column <code>data.grid_definition.draw_priority</code>.
     */
    public final TableField<GridDefinitionRecord, Integer> DRAW_PRIORITY = createField(DSL.name("draw_priority"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.grid_definition.outline_color</code>.
     */
    public final TableField<GridDefinitionRecord, String> OUTLINE_COLOR = createField(DSL.name("outline_color"), SQLDataType.VARCHAR(50), this, "");

    /**
     * The column <code>data.grid_definition.table_name</code>.
     */
    public final TableField<GridDefinitionRecord, String> TABLE_NAME = createField(DSL.name("table_name"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>data.grid_definition.user_id</code>.
     */
    public final TableField<GridDefinitionRecord, String> USER_ID = createField(DSL.name("user_id"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>data.grid_definition.share_scope</code>.
     */
    public final TableField<GridDefinitionRecord, Short> SHARE_SCOPE = createField(DSL.name("share_scope"), SQLDataType.SMALLINT.defaultValue(DSL.field("0", SQLDataType.SMALLINT)), this, "");

    /**
     * The column <code>data.grid_definition.task_log</code>.
     */
    public final TableField<GridDefinitionRecord, JSON> TASK_LOG = createField(DSL.name("task_log"), SQLDataType.JSON, this, "");

    /**
     * The column <code>data.grid_definition.archive</code>.
     */
    public final TableField<GridDefinitionRecord, Short> ARCHIVE = createField(DSL.name("archive"), SQLDataType.SMALLINT.defaultValue(DSL.field("0", SQLDataType.SMALLINT)), this, "");

    private GridDefinition(Name alias, Table<GridDefinitionRecord> aliased) {
        this(alias, aliased, null);
    }

    private GridDefinition(Name alias, Table<GridDefinitionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.grid_definition</code> table reference
     */
    public GridDefinition(String alias) {
        this(DSL.name(alias), GRID_DEFINITION);
    }

    /**
     * Create an aliased <code>data.grid_definition</code> table reference
     */
    public GridDefinition(Name alias) {
        this(alias, GRID_DEFINITION);
    }

    /**
     * Create a <code>data.grid_definition</code> table reference
     */
    public GridDefinition() {
        this(DSL.name("grid_definition"), null);
    }

    public <O extends Record> GridDefinition(Table<O> child, ForeignKey<O, GridDefinitionRecord> key) {
        super(child, key, GRID_DEFINITION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Data.DATA;
    }

    @Override
    public Identity<GridDefinitionRecord, Integer> getIdentity() {
        return (Identity<GridDefinitionRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<GridDefinitionRecord> getPrimaryKey() {
        return Keys.GRID_DEFINITION_PKEY;
    }

    @Override
    public GridDefinition as(String alias) {
        return new GridDefinition(DSL.name(alias), this);
    }

    @Override
    public GridDefinition as(Name alias) {
        return new GridDefinition(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public GridDefinition rename(String name) {
        return new GridDefinition(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public GridDefinition rename(Name name) {
        return new GridDefinition(name, null);
    }

    // -------------------------------------------------------------------------
    // Row12 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row12<Integer, String, Integer, Integer, String, Integer, String, String, String, Short, JSON, Short> fieldsRow() {
        return (Row12) super.fieldsRow();
    }
}
