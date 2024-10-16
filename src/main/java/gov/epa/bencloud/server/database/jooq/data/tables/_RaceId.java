/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.tables.records._RaceIdRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row1;
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
public class _RaceId extends TableImpl<_RaceIdRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data._race_id</code>
     */
    public static final _RaceId _RACE_ID = new _RaceId();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<_RaceIdRecord> getRecordType() {
        return _RaceIdRecord.class;
    }

    /**
     * The column <code>data._race_id.?column?</code>.
     */
    public final TableField<_RaceIdRecord, String> _3fCOLUMN_3f = createField(DSL.name("?column?"), SQLDataType.CLOB, this, "");

    private _RaceId(Name alias, Table<_RaceIdRecord> aliased) {
        this(alias, aliased, null);
    }

    private _RaceId(Name alias, Table<_RaceIdRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data._race_id</code> table reference
     */
    public _RaceId(String alias) {
        this(DSL.name(alias), _RACE_ID);
    }

    /**
     * Create an aliased <code>data._race_id</code> table reference
     */
    public _RaceId(Name alias) {
        this(alias, _RACE_ID);
    }

    /**
     * Create a <code>data._race_id</code> table reference
     */
    public _RaceId() {
        this(DSL.name("_race_id"), null);
    }

    public <O extends Record> _RaceId(Table<O> child, ForeignKey<O, _RaceIdRecord> key) {
        super(child, key, _RACE_ID);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Data.DATA;
    }

    @Override
    public _RaceId as(String alias) {
        return new _RaceId(DSL.name(alias), this);
    }

    @Override
    public _RaceId as(Name alias) {
        return new _RaceId(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public _RaceId rename(String name) {
        return new _RaceId(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public _RaceId rename(Name name) {
        return new _RaceId(name, null);
    }

    // -------------------------------------------------------------------------
    // Row1 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row1<String> fieldsRow() {
        return (Row1) super.fieldsRow();
    }
}