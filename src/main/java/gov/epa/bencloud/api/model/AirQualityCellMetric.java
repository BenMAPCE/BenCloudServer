package gov.epa.bencloud.api.model;

public class AirQualityCellMetric {
	private Integer annualStatistic;
	private double value; //TODO: This may need to become a list when we get to daily/hourly data
	private Integer metric;
	private Integer seasonalMetric;
	
	public AirQualityCellMetric(Integer metric, Integer seasonalMetric, Integer annualStatistic, double value) {
		super();

		this.metric = metric;
		this.seasonalMetric = seasonalMetric;
		this.annualStatistic = annualStatistic;
		this.value = value;
	}


	public Integer getAnnualStatistic() {
		return annualStatistic;
	}


	public void setAnnualStatistic(Integer annualStatistic) {
		this.annualStatistic = annualStatistic;
	}


	public Integer getMetric() {
		return metric;
	}


	public void setMetric(Integer metric) {
		this.metric = metric;
	}


	public Integer getSeasonalMetric() {
		return seasonalMetric;
	}


	public void setSeasonalMetric(Integer seasonalMetric) {
		this.seasonalMetric = seasonalMetric;
	}


	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
}
