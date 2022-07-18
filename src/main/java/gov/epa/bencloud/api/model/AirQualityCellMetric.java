package gov.epa.bencloud.api.model;

/*
 *	Representation of an air quality cell metric
 */
public class AirQualityCellMetric {
	private Integer annualStatistic;
	private double value; //TODO: This may need to become a list when we get to daily/hourly data
	private Integer metric;
	private Integer seasonalMetric;
	
	/**
	 * Creates an air quality cell metric object.
	 * @param metric Pollutant metric id
	 * @param seasonalMetric
	 * @param annualStatistic
	 * @param value
	 */
	public AirQualityCellMetric(Integer metric, Integer seasonalMetric, Integer annualStatistic, double value) {
		super();

		this.metric = metric;
		this.seasonalMetric = seasonalMetric;
		this.annualStatistic = annualStatistic;
		this.value = value;
	}


	/**
	 * @return air quality cell metric's annual statistic.
	 */
	public Integer getAnnualStatistic() {
		return annualStatistic;
	}

	/**
	 * Sets the air quality cell metric's annual statistic.
	 * @param annualStatistic
	 */
	public void setAnnualStatistic(Integer annualStatistic) {
		this.annualStatistic = annualStatistic;
	}

	/**
	 * @return air quality cell metric's pollutant metric.
	 */
	public Integer getMetric() {
		return metric;
	}

	/**
	 * Sets the air quality cell metric's pollutant metric.
	 * @param metric
	 */
	public void setMetric(Integer metric) {
		this.metric = metric;
	}


	/**
	 * 
	 * @return air quality cell metric's seasonal metric.
	 */
	public Integer getSeasonalMetric() {
		return seasonalMetric;
	}

	/**
	 * Sets the air quality cell metric's seasonal metric.
	 * @param seasonalMetric
	 */
	public void setSeasonalMetric(Integer seasonalMetric) {
		this.seasonalMetric = seasonalMetric;
	}


	/**
	 * 
	 * @return air quality cell metric's value.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Sets the air quality cell metric's value.
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;
	}
	
}
