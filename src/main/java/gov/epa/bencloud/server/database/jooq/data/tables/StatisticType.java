/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.Keys;
import gov.epa.bencloud.server.database.jooq.data.tables.records.StatisticTypeRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
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
public class StatisticType extends TableImpl<StatisticTypeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.statistic_type</code>
     */
    public static final StatisticType STATISTIC_TYPE = new StatisticType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StatisticTypeRecord> getRecordType() {
        return StatisticTypeRecord.class;
    }

    /**
     * The column <code>data.statistic_type.id</code>.
     */
    public final TableField<StatisticTypeRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>data.statistic_type.name</code>.
     */
    public final TableField<StatisticTypeRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB.nullable(false), this, "");

    private StatisticType(Name alias, Table<StatisticTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private StatisticType(Name alias, Table<StatisticTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.statistic_type</code> table reference
     */
    public StatisticType(String alias) {
        this(DSL.name(alias), STATISTIC_TYPE);
    }

    /**
     * Create an aliased <code>data.statistic_type</code> table reference
     */
    public StatisticType(Name alias) {
        this(alias, STATISTIC_TYPE);
    }

    /**
     * Create a <code>data.statistic_type</code> table reference
     */
    public StatisticType() {
        this(DSL.name("statistic_type"), null);
    }

    public <O extends Record> StatisticType(Table<O> child, ForeignKey<O, StatisticTypeRecord> key) {
        super(child, key, STATISTIC_TYPE);
    }

    @Override
    public Schema getSchema() {
        return Data.DATA;
    }

    @Override
    public Identity<StatisticTypeRecord, Integer> getIdentity() {
        return (Identity<StatisticTypeRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<StatisticTypeRecord> getPrimaryKey() {
        return Keys.STATISTIC_TYPE_PKEY;
    }

    @Override
    public List<UniqueKey<StatisticTypeRecord>> getKeys() {
        return Arrays.<UniqueKey<StatisticTypeRecord>>asList(Keys.STATISTIC_TYPE_PKEY);
    }

    @Override
    public StatisticType as(String alias) {
        return new StatisticType(DSL.name(alias), this);
    }

    @Override
    public StatisticType as(Name alias) {
        return new StatisticType(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public StatisticType rename(String name) {
        return new StatisticType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public StatisticType rename(Name name) {
        return new StatisticType(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}