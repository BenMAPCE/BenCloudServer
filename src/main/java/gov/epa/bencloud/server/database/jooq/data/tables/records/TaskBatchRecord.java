/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.TaskBatch;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskBatchRecord extends UpdatableRecordImpl<TaskBatchRecord> implements Record5<Integer, String, String, String, Short> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.task_batch.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.task_batch.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.task_batch.name</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.task_batch.name</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>data.task_batch.parameters</code>.
     */
    public void setParameters(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.task_batch.parameters</code>.
     */
    public String getParameters() {
        return (String) get(2);
    }

    /**
     * Setter for <code>data.task_batch.user_id</code>.
     */
    public void setUserId(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.task_batch.user_id</code>.
     */
    public String getUserId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>data.task_batch.sharing_scope</code>.
     */
    public void setSharingScope(Short value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.task_batch.sharing_scope</code>.
     */
    public Short getSharingScope() {
        return (Short) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, String, String, Short> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, String, String, String, Short> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return TaskBatch.TASK_BATCH.ID;
    }

    @Override
    public Field<String> field2() {
        return TaskBatch.TASK_BATCH.NAME;
    }

    @Override
    public Field<String> field3() {
        return TaskBatch.TASK_BATCH.PARAMETERS;
    }

    @Override
    public Field<String> field4() {
        return TaskBatch.TASK_BATCH.USER_ID;
    }

    @Override
    public Field<Short> field5() {
        return TaskBatch.TASK_BATCH.SHARING_SCOPE;
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
        return getParameters();
    }

    @Override
    public String component4() {
        return getUserId();
    }

    @Override
    public Short component5() {
        return getSharingScope();
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
        return getParameters();
    }

    @Override
    public String value4() {
        return getUserId();
    }

    @Override
    public Short value5() {
        return getSharingScope();
    }

    @Override
    public TaskBatchRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public TaskBatchRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public TaskBatchRecord value3(String value) {
        setParameters(value);
        return this;
    }

    @Override
    public TaskBatchRecord value4(String value) {
        setUserId(value);
        return this;
    }

    @Override
    public TaskBatchRecord value5(Short value) {
        setSharingScope(value);
        return this;
    }

    @Override
    public TaskBatchRecord values(Integer value1, String value2, String value3, String value4, Short value5) {
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
     * Create a detached TaskBatchRecord
     */
    public TaskBatchRecord() {
        super(TaskBatch.TASK_BATCH);
    }

    /**
     * Create a detached, initialised TaskBatchRecord
     */
    public TaskBatchRecord(Integer id, String name, String parameters, String userId, Short sharingScope) {
        super(TaskBatch.TASK_BATCH);

        setId(id);
        setName(name);
        setParameters(parameters);
        setUserId(userId);
        setSharingScope(sharingScope);
    }
}
