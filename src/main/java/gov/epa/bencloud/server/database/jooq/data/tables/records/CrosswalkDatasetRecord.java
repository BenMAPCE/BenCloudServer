/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.CrosswalkDataset;

import java.time.LocalDateTime;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CrosswalkDatasetRecord extends UpdatableRecordImpl<CrosswalkDatasetRecord> implements Record4<Integer, Integer, Integer, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.crosswalk_dataset.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.crosswalk_dataset.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.crosswalk_dataset.source_grid_id</code>.
     */
    public void setSourceGridId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.crosswalk_dataset.source_grid_id</code>.
     */
    public Integer getSourceGridId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>data.crosswalk_dataset.target_grid_id</code>.
     */
    public void setTargetGridId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.crosswalk_dataset.target_grid_id</code>.
     */
    public Integer getTargetGridId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>data.crosswalk_dataset.created_date</code>.
     */
    public void setCreatedDate(LocalDateTime value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.crosswalk_dataset.created_date</code>.
     */
    public LocalDateTime getCreatedDate() {
        return (LocalDateTime) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, Integer, Integer, LocalDateTime> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, Integer, Integer, LocalDateTime> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return CrosswalkDataset.CROSSWALK_DATASET.ID;
    }

    @Override
    public Field<Integer> field2() {
        return CrosswalkDataset.CROSSWALK_DATASET.SOURCE_GRID_ID;
    }

    @Override
    public Field<Integer> field3() {
        return CrosswalkDataset.CROSSWALK_DATASET.TARGET_GRID_ID;
    }

    @Override
    public Field<LocalDateTime> field4() {
        return CrosswalkDataset.CROSSWALK_DATASET.CREATED_DATE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getSourceGridId();
    }

    @Override
    public Integer component3() {
        return getTargetGridId();
    }

    @Override
    public LocalDateTime component4() {
        return getCreatedDate();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getSourceGridId();
    }

    @Override
    public Integer value3() {
        return getTargetGridId();
    }

    @Override
    public LocalDateTime value4() {
        return getCreatedDate();
    }

    @Override
    public CrosswalkDatasetRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public CrosswalkDatasetRecord value2(Integer value) {
        setSourceGridId(value);
        return this;
    }

    @Override
    public CrosswalkDatasetRecord value3(Integer value) {
        setTargetGridId(value);
        return this;
    }

    @Override
    public CrosswalkDatasetRecord value4(LocalDateTime value) {
        setCreatedDate(value);
        return this;
    }

    @Override
    public CrosswalkDatasetRecord values(Integer value1, Integer value2, Integer value3, LocalDateTime value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CrosswalkDatasetRecord
     */
    public CrosswalkDatasetRecord() {
        super(CrosswalkDataset.CROSSWALK_DATASET);
    }

    /**
     * Create a detached, initialised CrosswalkDatasetRecord
     */
    public CrosswalkDatasetRecord(Integer id, Integer sourceGridId, Integer targetGridId, LocalDateTime createdDate) {
        super(CrosswalkDataset.CROSSWALK_DATASET);

        setId(id);
        setSourceGridId(sourceGridId);
        setTargetGridId(targetGridId);
        setCreatedDate(createdDate);
    }
}
