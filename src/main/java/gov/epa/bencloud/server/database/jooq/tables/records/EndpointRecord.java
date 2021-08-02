/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.tables.records;


import gov.epa.bencloud.server.database.jooq.tables.Endpoint;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EndpointRecord extends UpdatableRecordImpl<EndpointRecord> implements Record3<Integer, Short, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.endpoint.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.endpoint.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.endpoint.endpoint_group_id</code>.
     */
    public void setEndpointGroupId(Short value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.endpoint.endpoint_group_id</code>.
     */
    public Short getEndpointGroupId() {
        return (Short) get(1);
    }

    /**
     * Setter for <code>data.endpoint.name</code>.
     */
    public void setName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.endpoint.name</code>.
     */
    public String getName() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Short, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, Short, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Endpoint.ENDPOINT.ID;
    }

    @Override
    public Field<Short> field2() {
        return Endpoint.ENDPOINT.ENDPOINT_GROUP_ID;
    }

    @Override
    public Field<String> field3() {
        return Endpoint.ENDPOINT.NAME;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Short component2() {
        return getEndpointGroupId();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Short value2() {
        return getEndpointGroupId();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public EndpointRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public EndpointRecord value2(Short value) {
        setEndpointGroupId(value);
        return this;
    }

    @Override
    public EndpointRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public EndpointRecord values(Integer value1, Short value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached EndpointRecord
     */
    public EndpointRecord() {
        super(Endpoint.ENDPOINT);
    }

    /**
     * Create a detached, initialised EndpointRecord
     */
    public EndpointRecord(Integer id, Short endpointGroupId, String name) {
        super(Endpoint.ENDPOINT);

        setId(id);
        setEndpointGroupId(endpointGroupId);
        setName(name);
    }
}