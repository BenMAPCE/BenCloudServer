package gov.epa.bencloud.api.util;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.AirQualityCellRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.PollutantMetricRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.SeasonalMetricRecord;

public class AirQualityUtil {

	public static String validateModelColumnHeadings(int columnIdx, int rowIdx, int metricIdx, int seasonalMetricIdx, int statisticIdx, int valuesIdx) {
		StringBuilder b = new StringBuilder();
		if(columnIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Column");
		}
		if(rowIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Row");
		}
		if(metricIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Metric");
		}
		if(seasonalMetricIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Seasonal Metric");
		}
		if(statisticIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Annual Metric");
		}
		if(valuesIdx == -999) {
			b.append((b.length()==0 ? "" : ", ") + "Values");
		}
		
		return b.toString();
	}

	public static Map<String, Integer> getPollutantMetricIdLookup(Integer pollutantId) {
		Map<String, Integer> pollutantMetrics = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(POLLUTANT_METRIC.NAME), POLLUTANT_METRIC.ID)
				.from(POLLUTANT_METRIC)
				.where(POLLUTANT_METRIC.POLLUTANT_ID.eq(pollutantId))
				.fetchMap(DSL.lower(POLLUTANT_METRIC.NAME), POLLUTANT_METRIC.ID);		
		return pollutantMetrics;
	}

	public static Integer createNewPollutantMetric(Integer pollutantId, String metricName) {

		PollutantMetricRecord metricRecord = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(POLLUTANT_METRIC, POLLUTANT_METRIC.POLLUTANT_ID, POLLUTANT_METRIC.NAME)
		.values(pollutantId, metricName)
		.returning(POLLUTANT_METRIC.ID)
		.fetchOne();
		
		return metricRecord.getId();
	}

	public static Map<String, Integer> getSeasonalMetricIdLookup(Integer pollutantId) {
		Map<String, Integer> pollutantSeasonalMetrics = DSL.using(JooqUtil.getJooqConfiguration())
				.select(DSL.lower(DSL.concat(POLLUTANT_METRIC.NAME, DSL.val("~"), SEASONAL_METRIC.NAME)), SEASONAL_METRIC.ID)
				.from(POLLUTANT_METRIC)
				.join(SEASONAL_METRIC).on(SEASONAL_METRIC.METRIC_ID.eq(POLLUTANT_METRIC.ID))
				.where(POLLUTANT_METRIC.POLLUTANT_ID.eq(pollutantId))
				.fetchMap(DSL.lower(DSL.concat(POLLUTANT_METRIC.NAME, DSL.val("~"), SEASONAL_METRIC.NAME)), SEASONAL_METRIC.ID);		
		return pollutantSeasonalMetrics;	}

	public static Integer createNewSeasonalMetric(Integer metricId, String seasonalMetricName) {
		SeasonalMetricRecord metricRecord = DSL.using(JooqUtil.getJooqConfiguration())
		.insertInto(SEASONAL_METRIC, SEASONAL_METRIC.METRIC_ID, SEASONAL_METRIC.NAME)
		.values(metricId, seasonalMetricName)
		.returning(SEASONAL_METRIC.ID)
		.fetchOne();
		
		return metricRecord.getId();
	}

	public static List<Integer> getSupportedMetricIds(int baselineId, int scenarioId) {
		List<Integer> metricIds = new ArrayList<Integer>();
		// Return list of metrics supported by this set of air quality layers.

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());
		Integer[] baselineMetrics = create.selectDistinct(AIR_QUALITY_CELL.METRIC_ID)
				.from(AIR_QUALITY_CELL)
				.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(baselineId))
				.fetchArray(AIR_QUALITY_CELL.METRIC_ID);

		Integer[] scenarioMetrics = create.selectDistinct(AIR_QUALITY_CELL.METRIC_ID)
				.from(AIR_QUALITY_CELL)
				.where(AIR_QUALITY_CELL.AIR_QUALITY_LAYER_ID.eq(scenarioId))
				.fetchArray(AIR_QUALITY_CELL.METRIC_ID);
		
		for(Integer baselineMetric : baselineMetrics) {
			for(Integer scenarioMetric : scenarioMetrics) {
				if(baselineMetric.equals(scenarioMetric)) {
					metricIds.add(baselineMetric);				
				}
			}
		}

		return metricIds;
	}

	public static String getPollutantMetricList(Integer id) {
		String[] pollutantMetrics = DSL.using(JooqUtil.getJooqConfiguration())
				.select(POLLUTANT_METRIC.NAME)
				.from(POLLUTANT_METRIC)
				.where(POLLUTANT_METRIC.POLLUTANT_ID.eq(id))
				.orderBy(POLLUTANT_METRIC.NAME)
				.fetchArray(POLLUTANT_METRIC.NAME);		
		
		
		return String.join(", ", pollutantMetrics);
	}
	
	public static List<String> getExistingLayerNames(Integer id) {
		List<String> layerNames = new ArrayList<String>();
		// Return list of AQ layer names (converted to lower cases) for this pollutant id.

		DSLContext create = DSL.using(JooqUtil.getJooqConfiguration());		
		layerNames = create.select(DSL.lower(AIR_QUALITY_LAYER.NAME)).from(AIR_QUALITY_LAYER)
				.where(AIR_QUALITY_LAYER.POLLUTANT_ID.eq(id))
				.fetch(DSL.lower(AIR_QUALITY_LAYER.NAME));

		return layerNames;
	}

}
