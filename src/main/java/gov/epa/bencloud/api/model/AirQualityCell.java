package gov.epa.bencloud.api.model;

import java.util.HashMap;
import java.util.Map;

public class AirQualityCell {
	private Integer gridCol;
	private Integer gridRow;

	private Map<Integer, Map<Integer, AirQualityCellMetric>> cellMetrics; //key1=metric, key2=seasonalMetric
	
	public AirQualityCell(Integer col, Integer row) {
		this.gridCol = col;
		this.gridRow = row;
		this.cellMetrics = new HashMap<Integer, Map<Integer, AirQualityCellMetric>>();
	}

	public Integer getGridCol() {
		return gridCol;
	}

	public void setGridCol(Integer gridCol) {
		this.gridCol = gridCol;
	}

	public Integer getGridRow() {
		return gridRow;
	}

	public void setGridRow(Integer gridRow) {
		this.gridRow = gridRow;
	}

	public Map<Integer, Map<Integer, AirQualityCellMetric>> getCellMetrics() {
		return cellMetrics;
	}

	public void setCellMetrics(Map<Integer, Map<Integer, AirQualityCellMetric>> cellMetrics) {
		this.cellMetrics = cellMetrics;
	}



}
