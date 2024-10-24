/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.data.tables.records;


import gov.epa.bencloud.server.database.jooq.data.tables.GetValuationResults;

import org.jooq.Field;
import org.jooq.Record12;
import org.jooq.Row12;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class GetValuationResultsRecord extends TableRecordImpl<GetValuationResultsRecord> implements Record12<Integer, Integer, Integer, Integer, Integer, Double, Double, Double, Double, Double, Double, Double[]> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>data.get_valuation_results.grid_col</code>.
     */
    public void setGridCol(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.grid_col</code>.
     */
    public Integer getGridCol() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>data.get_valuation_results.grid_row</code>.
     */
    public void setGridRow(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.grid_row</code>.
     */
    public Integer getGridRow() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>data.get_valuation_results.hif_id</code>.
     */
    public void setHifId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.hif_id</code>.
     */
    public Integer getHifId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>data.get_valuation_results.hif_instance_id</code>.
     */
    public void setHifInstanceId(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.hif_instance_id</code>.
     */
    public Integer getHifInstanceId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>data.get_valuation_results.vf_id</code>.
     */
    public void setVfId(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.vf_id</code>.
     */
    public Integer getVfId() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>data.get_valuation_results.point_estimate</code>.
     */
    public void setPointEstimate(Double value) {
        set(5, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.point_estimate</code>.
     */
    public Double getPointEstimate() {
        return (Double) get(5);
    }

    /**
     * Setter for <code>data.get_valuation_results.mean</code>.
     */
    public void setMean(Double value) {
        set(6, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.mean</code>.
     */
    public Double getMean() {
        return (Double) get(6);
    }

    /**
     * Setter for <code>data.get_valuation_results.standard_dev</code>.
     */
    public void setStandardDev(Double value) {
        set(7, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.standard_dev</code>.
     */
    public Double getStandardDev() {
        return (Double) get(7);
    }

    /**
     * Setter for <code>data.get_valuation_results.variance</code>.
     */
    public void setVariance(Double value) {
        set(8, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.variance</code>.
     */
    public Double getVariance() {
        return (Double) get(8);
    }

    /**
     * Setter for <code>data.get_valuation_results.pct_2_5</code>.
     */
    public void setPct_2_5(Double value) {
        set(9, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.pct_2_5</code>.
     */
    public Double getPct_2_5() {
        return (Double) get(9);
    }

    /**
     * Setter for <code>data.get_valuation_results.pct_97_5</code>.
     */
    public void setPct_97_5(Double value) {
        set(10, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.pct_97_5</code>.
     */
    public Double getPct_97_5() {
        return (Double) get(10);
    }

    /**
     * Setter for <code>data.get_valuation_results.percentiles</code>.
     */
    public void setPercentiles(Double[] value) {
        set(11, value);
    }

    /**
     * Getter for <code>data.get_valuation_results.percentiles</code>.
     */
    public Double[] getPercentiles() {
        return (Double[]) get(11);
    }

    // -------------------------------------------------------------------------
    // Record12 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row12<Integer, Integer, Integer, Integer, Integer, Double, Double, Double, Double, Double, Double, Double[]> fieldsRow() {
        return (Row12) super.fieldsRow();
    }

    @Override
    public Row12<Integer, Integer, Integer, Integer, Integer, Double, Double, Double, Double, Double, Double, Double[]> valuesRow() {
        return (Row12) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return GetValuationResults.GET_VALUATION_RESULTS.GRID_COL;
    }

    @Override
    public Field<Integer> field2() {
        return GetValuationResults.GET_VALUATION_RESULTS.GRID_ROW;
    }

    @Override
    public Field<Integer> field3() {
        return GetValuationResults.GET_VALUATION_RESULTS.HIF_ID;
    }

    @Override
    public Field<Integer> field4() {
        return GetValuationResults.GET_VALUATION_RESULTS.HIF_INSTANCE_ID;
    }

    @Override
    public Field<Integer> field5() {
        return GetValuationResults.GET_VALUATION_RESULTS.VF_ID;
    }

    @Override
    public Field<Double> field6() {
        return GetValuationResults.GET_VALUATION_RESULTS.POINT_ESTIMATE;
    }

    @Override
    public Field<Double> field7() {
        return GetValuationResults.GET_VALUATION_RESULTS.MEAN;
    }

    @Override
    public Field<Double> field8() {
        return GetValuationResults.GET_VALUATION_RESULTS.STANDARD_DEV;
    }

    @Override
    public Field<Double> field9() {
        return GetValuationResults.GET_VALUATION_RESULTS.VARIANCE;
    }

    @Override
    public Field<Double> field10() {
        return GetValuationResults.GET_VALUATION_RESULTS.PCT_2_5;
    }

    @Override
    public Field<Double> field11() {
        return GetValuationResults.GET_VALUATION_RESULTS.PCT_97_5;
    }

    @Override
    public Field<Double[]> field12() {
        return GetValuationResults.GET_VALUATION_RESULTS.PERCENTILES;
    }

    @Override
    public Integer component1() {
        return getGridCol();
    }

    @Override
    public Integer component2() {
        return getGridRow();
    }

    @Override
    public Integer component3() {
        return getHifId();
    }

    @Override
    public Integer component4() {
        return getHifInstanceId();
    }

    @Override
    public Integer component5() {
        return getVfId();
    }

    @Override
    public Double component6() {
        return getPointEstimate();
    }

    @Override
    public Double component7() {
        return getMean();
    }

    @Override
    public Double component8() {
        return getStandardDev();
    }

    @Override
    public Double component9() {
        return getVariance();
    }

    @Override
    public Double component10() {
        return getPct_2_5();
    }

    @Override
    public Double component11() {
        return getPct_97_5();
    }

    @Override
    public Double[] component12() {
        return getPercentiles();
    }

    @Override
    public Integer value1() {
        return getGridCol();
    }

    @Override
    public Integer value2() {
        return getGridRow();
    }

    @Override
    public Integer value3() {
        return getHifId();
    }

    @Override
    public Integer value4() {
        return getHifInstanceId();
    }

    @Override
    public Integer value5() {
        return getVfId();
    }

    @Override
    public Double value6() {
        return getPointEstimate();
    }

    @Override
    public Double value7() {
        return getMean();
    }

    @Override
    public Double value8() {
        return getStandardDev();
    }

    @Override
    public Double value9() {
        return getVariance();
    }

    @Override
    public Double value10() {
        return getPct_2_5();
    }

    @Override
    public Double value11() {
        return getPct_97_5();
    }

    @Override
    public Double[] value12() {
        return getPercentiles();
    }

    @Override
    public GetValuationResultsRecord value1(Integer value) {
        setGridCol(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value2(Integer value) {
        setGridRow(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value3(Integer value) {
        setHifId(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value4(Integer value) {
        setHifInstanceId(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value5(Integer value) {
        setVfId(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value6(Double value) {
        setPointEstimate(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value7(Double value) {
        setMean(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value8(Double value) {
        setStandardDev(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value9(Double value) {
        setVariance(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value10(Double value) {
        setPct_2_5(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value11(Double value) {
        setPct_97_5(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord value12(Double[] value) {
        setPercentiles(value);
        return this;
    }

    @Override
    public GetValuationResultsRecord values(Integer value1, Integer value2, Integer value3, Integer value4, Integer value5, Double value6, Double value7, Double value8, Double value9, Double value10, Double value11, Double[] value12) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached GetValuationResultsRecord
     */
    public GetValuationResultsRecord() {
        super(GetValuationResults.GET_VALUATION_RESULTS);
    }

    /**
     * Create a detached, initialised GetValuationResultsRecord
     */
    public GetValuationResultsRecord(Integer gridCol, Integer gridRow, Integer hifId, Integer hifInstanceId, Integer vfId, Double pointEstimate, Double mean, Double standardDev, Double variance, Double pct_2_5, Double pct_97_5, Double[] percentiles) {
        super(GetValuationResults.GET_VALUATION_RESULTS);

        setGridCol(gridCol);
        setGridRow(gridRow);
        setHifId(hifId);
        setHifInstanceId(hifInstanceId);
        setVfId(vfId);
        setPointEstimate(pointEstimate);
        setMean(mean);
        setStandardDev(standardDev);
        setVariance(variance);
        setPct_2_5(pct_2_5);
        setPct_97_5(pct_97_5);
        setPercentiles(percentiles);
    }
}
