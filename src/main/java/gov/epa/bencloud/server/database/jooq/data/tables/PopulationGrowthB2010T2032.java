/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables;


import gov.epa.bencloud.server.database.jooq.data.Data;
import gov.epa.bencloud.server.database.jooq.data.Indexes;
import gov.epa.bencloud.server.database.jooq.data.Keys;
import gov.epa.bencloud.server.database.jooq.data.tables.records.PopulationGrowthB2010T2032Record;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row8;
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
public class PopulationGrowthB2010T2032 extends TableImpl<PopulationGrowthB2010T2032Record> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>data.population_growth_b2010_t2032</code>
     */
    public static final PopulationGrowthB2010T2032 POPULATION_GROWTH_B2010_T2032 = new PopulationGrowthB2010T2032();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PopulationGrowthB2010T2032Record> getRecordType() {
        return PopulationGrowthB2010T2032Record.class;
    }

    /**
     * The column <code>data.population_growth_b2010_t2032.base_pop_year</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Short> BASE_POP_YEAR = createField(DSL.name("base_pop_year"), SQLDataType.SMALLINT.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.pop_year</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Short> POP_YEAR = createField(DSL.name("pop_year"), SQLDataType.SMALLINT.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.race_id</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Integer> RACE_ID = createField(DSL.name("race_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.gender_id</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Integer> GENDER_ID = createField(DSL.name("gender_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.ethnicity_id</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Integer> ETHNICITY_ID = createField(DSL.name("ethnicity_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.age_range_id</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Integer> AGE_RANGE_ID = createField(DSL.name("age_range_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.grid_cell_id</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Integer> GRID_CELL_ID = createField(DSL.name("grid_cell_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>data.population_growth_b2010_t2032.growth_value</code>.
     */
    public final TableField<PopulationGrowthB2010T2032Record, Double> GROWTH_VALUE = createField(DSL.name("growth_value"), SQLDataType.DOUBLE, this, "");

    private PopulationGrowthB2010T2032(Name alias, Table<PopulationGrowthB2010T2032Record> aliased) {
        this(alias, aliased, null);
    }

    private PopulationGrowthB2010T2032(Name alias, Table<PopulationGrowthB2010T2032Record> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>data.population_growth_b2010_t2032</code> table
     * reference
     */
    public PopulationGrowthB2010T2032(String alias) {
        this(DSL.name(alias), POPULATION_GROWTH_B2010_T2032);
    }

    /**
     * Create an aliased <code>data.population_growth_b2010_t2032</code> table
     * reference
     */
    public PopulationGrowthB2010T2032(Name alias) {
        this(alias, POPULATION_GROWTH_B2010_T2032);
    }

    /**
     * Create a <code>data.population_growth_b2010_t2032</code> table reference
     */
    public PopulationGrowthB2010T2032() {
        this(DSL.name("population_growth_b2010_t2032"), null);
    }

    public <O extends Record> PopulationGrowthB2010T2032(Table<O> child, ForeignKey<O, PopulationGrowthB2010T2032Record> key) {
        super(child, key, POPULATION_GROWTH_B2010_T2032);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Data.DATA;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.POPULATION_GROWTH_B2010_T2032_BASE_POP_YEAR_IDX, Indexes.POPULATION_GROWTH_B2010_T2032_RACE_ID_ETHNICITY_ID_GRID_CE_IDX1, Indexes.POPULATION_GROWTH_B2010_T2032_RACE_ID_ETHNICITY_ID_GRID_CEL_IDX);
    }

    @Override
    public UniqueKey<PopulationGrowthB2010T2032Record> getPrimaryKey() {
        return Keys.POPULATION_GROWTH_B2010_T2032_PKEY;
    }

    @Override
    public PopulationGrowthB2010T2032 as(String alias) {
        return new PopulationGrowthB2010T2032(DSL.name(alias), this);
    }

    @Override
    public PopulationGrowthB2010T2032 as(Name alias) {
        return new PopulationGrowthB2010T2032(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public PopulationGrowthB2010T2032 rename(String name) {
        return new PopulationGrowthB2010T2032(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public PopulationGrowthB2010T2032 rename(Name name) {
        return new PopulationGrowthB2010T2032(name, null);
    }

    // -------------------------------------------------------------------------
    // Row8 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row8<Short, Short, Integer, Integer, Integer, Integer, Integer, Double> fieldsRow() {
        return (Row8) super.fieldsRow();
    }
}
