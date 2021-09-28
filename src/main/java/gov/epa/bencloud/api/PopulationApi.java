package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.ArrayList;
import java.util.Map;

import org.jooq.JSONFormat;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.impl.DSL;

import gov.epa.bencloud.api.model.HIFConfig;
import gov.epa.bencloud.api.model.HIFTaskConfig;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.Routines;
import gov.epa.bencloud.server.database.jooq.data.tables.records.GetPopulationRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.HealthImpactFunctionRecord;
import spark.Response;

public class PopulationApi {

	public static Map<Integer, Result<GetPopulationRecord>> getPopulationEntryGroups(ArrayList<HealthImpactFunctionRecord> hifDefinitionList, HIFTaskConfig hifTaskConfig) {

		//TODO: Need to grow the population using the selected popYear
		//NOTE: For now, we're focusing on age groups and not dealing with race, gender, ethnicity
		
		// Get the array of age ranges to include based on the configured hifs
		ArrayList<Integer> ageRangeIds = getAgeRangesForHifs(hifTaskConfig);
        Integer arrAgeRangeIds[] = new Integer[ageRangeIds.size()];
        arrAgeRangeIds = ageRangeIds.toArray(arrAgeRangeIds);
        
		Map<Integer, Result<GetPopulationRecord>> popRecords = Routines.getPopulation(JooqUtil.getJooqConfiguration(), 
				hifTaskConfig.popId, 
				hifTaskConfig.popYear,
				null, 
				null, 
				null, 
				arrAgeRangeIds, 
				null, 
				null, 
				null, 
				true, 
				28).intoGroups(GET_POPULATION.GRID_CELL_ID);

		return popRecords;
	}

	private static ArrayList<Integer> getAgeRangesForHifs(HIFTaskConfig hifTaskConfig) {
		
		int minHifAge = 999;
		int maxHifAge = 0;
		
		for(HIFConfig hif : hifTaskConfig.hifs) {
			minHifAge = hif.startAge < minHifAge ? hif.startAge : minHifAge;
			maxHifAge = hif.endAge > maxHifAge ? hif.endAge : maxHifAge;
		}
		
		Record1<Integer> popConfig = DSL.using(JooqUtil.getJooqConfiguration())
		.select(POPULATION_DATASET.POP_CONFIG_ID)
		.from(POPULATION_DATASET)
		.where(POPULATION_DATASET.ID.eq(hifTaskConfig.popId))
		.fetchOne();
		
		
		Result<Record3<Integer, Short, Short>> popAgeRanges = DSL.using(JooqUtil.getJooqConfiguration())
				.select(AGE_RANGE.ID, AGE_RANGE.START_AGE, AGE_RANGE.END_AGE)
				.from(AGE_RANGE)
				.where(AGE_RANGE.POP_CONFIG_ID.eq(popConfig.value1())
						.and(AGE_RANGE.END_AGE.greaterOrEqual((short) minHifAge))
						.and(AGE_RANGE.START_AGE.lessOrEqual((short) maxHifAge))
						)
				.fetch();
		
		ArrayList<Integer> ageRangeIds = new  ArrayList<Integer>();
		
		for(Record3<Integer, Short, Short> ageRange : popAgeRanges) {
			ageRangeIds.add(ageRange.value1());
		}
		
		return ageRangeIds;
	}

	public static Result<Record3<Integer, Short, Short>> getPopAgeRanges(Integer id) {

		Record1<Integer> popConfig = DSL.using(JooqUtil.getJooqConfiguration())
		.select(POPULATION_DATASET.POP_CONFIG_ID)
		.from(POPULATION_DATASET)
		.where(POPULATION_DATASET.ID.eq(id))
		.fetchOne();
		
		Result<Record3<Integer, Short, Short>> popAgeRanges = DSL.using(JooqUtil.getJooqConfiguration())
				.select(AGE_RANGE.ID, AGE_RANGE.START_AGE, AGE_RANGE.END_AGE)
				.from(AGE_RANGE)
				.where(AGE_RANGE.POP_CONFIG_ID.eq(popConfig.value1()))
				.fetch();
		
		return popAgeRanges;
	}
	
	public static Object getAllPopulationDatasets(Response response) {

			Result<Record4<String, Integer, Integer, Short[]>> records = DSL.using(JooqUtil.getJooqConfiguration())
					.select(POPULATION_DATASET.NAME,
							POPULATION_DATASET.ID,
							POPULATION_DATASET.GRID_DEFINITION_ID,
							DSL.arrayAggDistinct(POPULATION_ENTRY.POP_YEAR).orderBy(POPULATION_ENTRY.POP_YEAR).as("years"))
					.from(POPULATION_DATASET)
					.join(POPULATION_ENTRY).on(POPULATION_DATASET.ID.eq(POPULATION_ENTRY.POP_DATASET_ID))
					.groupBy(POPULATION_DATASET.NAME,
							POPULATION_DATASET.ID,
							POPULATION_DATASET.GRID_DEFINITION_ID)
					.orderBy(POPULATION_DATASET.NAME)
					.fetch();
			
			response.type("application/json");
			return records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));

	}
}
