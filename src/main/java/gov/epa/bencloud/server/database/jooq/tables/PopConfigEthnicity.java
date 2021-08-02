/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.tables;


import gov.epa.bencloud.server.database.jooq.Data;
import gov.epa.bencloud.server.database.jooq.Keys;
import gov.epa.bencloud.server.database.jooq.tables.records.PopConfigEthnicityRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
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
public class PopConfigEthnicity extends TableImpl<PopConfigEthnicityRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.pop_config_ethnicity</code>
     */
    public static final PopConfigEthnicity POP_CONFIG_ETHNICITY = new PopConfigEthnicity();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PopConfigEthnicityRecord> getRecordType() {
        return PopConfigEthnicityRecord.class;
    }

    /**
     * The column <code>data.pop_config_ethnicity.pop_config_id</code>.
     */
    public final TableField<PopConfigEthnicityRecord, Integer> POP_CONFIG_ID = createField(DSL.name("pop_config_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>data.pop_config_ethnicity.ethnicity_id</code>.
     */
    public final TableField<PopConfigEthnicityRecord, Integer> ETHNICITY_ID = createField(DSL.name("ethnicity_id"), SQLDataType.INTEGER, this, "");

    private PopConfigEthnicity(Name alias, Table<PopConfigEthnicityRecord> aliased) {
        this(alias, aliased, null);
    }

    private PopConfigEthnicity(Name alias, Table<PopConfigEthnicityRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.pop_config_ethnicity</code> table reference
     */
    public PopConfigEthnicity(String alias) {
        this(DSL.name(alias), POP_CONFIG_ETHNICITY);
    }

    /**
     * Create an aliased <code>data.pop_config_ethnicity</code> table reference
     */
    public PopConfigEthnicity(Name alias) {
        this(alias, POP_CONFIG_ETHNICITY);
    }

    /**
     * Create a <code>data.pop_config_ethnicity</code> table reference
     */
    public PopConfigEthnicity() {
        this(DSL.name("pop_config_ethnicity"), null);
    }

    public <O extends Record> PopConfigEthnicity(Table<O> child, ForeignKey<O, PopConfigEthnicityRecord> key) {
        super(child, key, POP_CONFIG_ETHNICITY);
    }

    @Override
    public Schema getSchema() {
        return Data.DATA;
    }

    @Override
    public List<ForeignKey<PopConfigEthnicityRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<PopConfigEthnicityRecord, ?>>asList(Keys.POP_CONFIG_ETHNICITY__POP_CONFIG_ETHNICITY_POP_CONFIG_ID_FKEY, Keys.POP_CONFIG_ETHNICITY__POP_CONFIG_ETHNICITY_ETHNICITY_ID_FKEY);
    }

    private transient PopConfig _popConfig;
    private transient Ethnicity _ethnicity;

    public PopConfig popConfig() {
        if (_popConfig == null)
            _popConfig = new PopConfig(this, Keys.POP_CONFIG_ETHNICITY__POP_CONFIG_ETHNICITY_POP_CONFIG_ID_FKEY);

        return _popConfig;
    }

    public Ethnicity ethnicity() {
        if (_ethnicity == null)
            _ethnicity = new Ethnicity(this, Keys.POP_CONFIG_ETHNICITY__POP_CONFIG_ETHNICITY_ETHNICITY_ID_FKEY);

        return _ethnicity;
    }

    @Override
    public PopConfigEthnicity as(String alias) {
        return new PopConfigEthnicity(DSL.name(alias), this);
    }

    @Override
    public PopConfigEthnicity as(Name alias) {
        return new PopConfigEthnicity(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public PopConfigEthnicity rename(String name) {
        return new PopConfigEthnicity(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public PopConfigEthnicity rename(Name name) {
        return new PopConfigEthnicity(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}