/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.Keys;
import gov.epa.bencloud.server.database.jooq.data.tables.records.ExposureResultDatasetRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.JSON;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row11;
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
public class ExposureResultDataset extends TableImpl<ExposureResultDatasetRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.exposure_result_dataset</code>
     */
    public static final ExposureResultDataset EXPOSURE_RESULT_DATASET = new ExposureResultDataset();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ExposureResultDatasetRecord> getRecordType() {
        return ExposureResultDatasetRecord.class;
    }

    /**
     * The column <code>data.exposure_result_dataset.id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>data.exposure_result_dataset.task_uuid</code>.
     */
    public final TableField<ExposureResultDatasetRecord, String> TASK_UUID = createField(DSL.name("task_uuid"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>data.exposure_result_dataset.name</code>.
     */
    public final TableField<ExposureResultDatasetRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB, this, "");

    /**
     * The column
     * <code>data.exposure_result_dataset.population_dataset_id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> POPULATION_DATASET_ID = createField(DSL.name("population_dataset_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.exposure_result_dataset.population_year</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> POPULATION_YEAR = createField(DSL.name("population_year"), SQLDataType.INTEGER, this, "");

    /**
     * The column
     * <code>data.exposure_result_dataset.baseline_aq_layer_id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> BASELINE_AQ_LAYER_ID = createField(DSL.name("baseline_aq_layer_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column
     * <code>data.exposure_result_dataset.scenario_aq_layer_id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> SCENARIO_AQ_LAYER_ID = createField(DSL.name("scenario_aq_layer_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.exposure_result_dataset.task_log</code>.
     */
    public final TableField<ExposureResultDatasetRecord, JSON> TASK_LOG = createField(DSL.name("task_log"), SQLDataType.JSON, this, "");

    /**
     * The column <code>data.exposure_result_dataset.user_id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, String> USER_ID = createField(DSL.name("user_id"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>data.exposure_result_dataset.sharing_scope</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Short> SHARING_SCOPE = createField(DSL.name("sharing_scope"), SQLDataType.SMALLINT.defaultValue(DSL.field("0", SQLDataType.SMALLINT)), this, "");

    /**
     * The column <code>data.exposure_result_dataset.grid_definition_id</code>.
     */
    public final TableField<ExposureResultDatasetRecord, Integer> GRID_DEFINITION_ID = createField(DSL.name("grid_definition_id"), SQLDataType.INTEGER, this, "");

    private ExposureResultDataset(Name alias, Table<ExposureResultDatasetRecord> aliased) {
        this(alias, aliased, null);
    }

    private ExposureResultDataset(Name alias, Table<ExposureResultDatasetRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.exposure_result_dataset</code> table
     * reference
     */
    public ExposureResultDataset(String alias) {
        this(DSL.name(alias), EXPOSURE_RESULT_DATASET);
    }

    /**
     * Create an aliased <code>data.exposure_result_dataset</code> table
     * reference
     */
    public ExposureResultDataset(Name alias) {
        this(alias, EXPOSURE_RESULT_DATASET);
    }

    /**
     * Create a <code>data.exposure_result_dataset</code> table reference
     */
    public ExposureResultDataset() {
        this(DSL.name("exposure_result_dataset"), null);
    }

    public <O extends Record> ExposureResultDataset(Table<O> child, ForeignKey<O, ExposureResultDatasetRecord> key) {
        super(child, key, EXPOSURE_RESULT_DATASET);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Data.DATA;
    }

    @Override
    public Identity<ExposureResultDatasetRecord, Integer> getIdentity() {
        return (Identity<ExposureResultDatasetRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ExposureResultDatasetRecord> getPrimaryKey() {
        return Keys.EXPOSURE_RESULT_DATASET_PKEY;
    }

    @Override
    public ExposureResultDataset as(String alias) {
        return new ExposureResultDataset(DSL.name(alias), this);
    }

    @Override
    public ExposureResultDataset as(Name alias) {
        return new ExposureResultDataset(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ExposureResultDataset rename(String name) {
        return new ExposureResultDataset(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ExposureResultDataset rename(Name name) {
        return new ExposureResultDataset(name, null);
    }

    // -------------------------------------------------------------------------
    // Row11 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row11<Integer, String, String, Integer, Integer, Integer, Integer, JSON, String, Short, Integer> fieldsRow() {
        return (Row11) super.fieldsRow();
    }
}