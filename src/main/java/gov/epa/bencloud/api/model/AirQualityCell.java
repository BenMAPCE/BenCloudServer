package gov.epa.bencloud.api.model;

import java.util.HashMap;
import java.util.Map;

/*
 *	Representation of a single air quality cell.
 */
public class AirQualityCell {
	private Integer gridCol;
	private Integer gridRow;

	private Map<Integer, Map<Integer, AirQualityCellMetric>> cellMetrics; //key1=metric, key2=seasonalMetric
	
	/**
	 * Creates an air quality cell object from the given grid col and grid row.
	 * @param col
	 * @param row
	 */
	public AirQualityCell(Integer col, Integer row) {
		this.gridCol = col;
		this.gridRow = row;
		this.cellMetrics = new HashMap<Integer, Map<Integer, AirQualityCellMetric>>();
	}

	/**
	 * 
	 * @return air quality cell's grid column.
	 */
	public Integer getGridCol() {
		return gridCol;
	}

	/**
	 * Sets the air quality cell's grid column.
	 * @param gridCol
	 */
	public void setGridCol(Integer gridCol) {
		this.gridCol = gridCol;
	}

	/**
	 * 
	 * @return air quality cell's grid row.
	 */ 
	public Integer getGridRow() {
		return gridRow;
	}

	/**
	 * Sets the air quality cell's grid row.
	 * @param gridRow
	 */
	public void setGridRow(Integer gridRow) {
		this.gridRow = gridRow;
	}

	/**
	 * 
	 * @return air quality cell's cell metrics.
	 */
	public Map<Integer, Map<Integer, AirQualityCellMetric>> getCellMetrics() {
		return cellMetrics;
	}

	/**
	 * Sets the air quality cell's cell metrics.
	 * @param cellMetrics
	 */
	public void setCellMetrics(Map<Integer, Map<Integer, AirQualityCellMetric>> cellMetrics) {
		this.cellMetrics = cellMetrics;
	}



}
