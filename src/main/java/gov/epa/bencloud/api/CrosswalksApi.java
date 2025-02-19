package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Arrays;
import java.util.List;

import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkDatasetRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.SelectConditionStep;

public class CrosswalksApi {
	private static final Logger log = LoggerFactory.getLogger(CrosswalksApi.class);

	/**
	 * calculates the crosswalks between two different grid definitions in both
	 * directions
	 * 
	 * @param grid_id1
	 * @param grid_id2
	 */
	public static boolean calculateAreaWeights(int grid_id1, int grid_id2) {

		Integer forwardCrosswalkID = null;
		boolean success = true;

		try {
			DSLContext dslContext = DSL.using(JooqUtil.getJooqConfiguration());
			
			CrosswalkDatasetRecord fwCrosswalkDatasetRecord = dslContext
					.insertInto(CROSSWALK_DATASET, CROSSWALK_DATASET.SOURCE_GRID_ID, CROSSWALK_DATASET.TARGET_GRID_ID)
					.values(grid_id1, grid_id2).returning(CROSSWALK_DATASET.ID).fetchOne();

			// store the id
			forwardCrosswalkID = fwCrosswalkDatasetRecord.value1();

			// get the grid names from the grid definition table for the given grid ids
			Record1<String> g1 = dslContext.select(GRID_DEFINITION.TABLE_NAME)
					.from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id1)).fetchOne();
			
			Record1<String> g2 = dslContext.select(GRID_DEFINITION.TABLE_NAME)
					.from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id2)).fetchOne();

			String g1Name = g1.value1();
			String g2Name = g2.value1();

			// get the grid SRID
			Integer g1Srid = dslContext
					.select(DSL.field("st_srid(geom)",Integer.class))
					.from(g1Name)
					.limit(1)
					.fetchOne()
					.value1();

			// create a table with the required fields and crosswalks for insertion into crosswalk entry table
			SelectConditionStep<Record8<Integer, Integer, Integer, Long, Integer, Integer, Long, Double>> fwQuery = dslContext
					.select(DSL.value(forwardCrosswalkID).as("crosswalk_id"),
							DSL.field(g1Name + ".col", Integer.class).as("source_col"),
							DSL.field(g1Name + ".row", Integer.class).as("source_row"),
							DSL.field("((" + DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + ")*("
									+ DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + "+1)*0.5)+"
									+ DSL.field(g1Name + ".row"), Long.class).as("source_grid_cell_id"), // same algorithm as ApiUtil.getCellId
							DSL.field("g2.col", Integer.class).as("target_col"),
							DSL.field("g2.row", Integer.class).as("target_row"),
							DSL.field("((" + DSL.field("g2.col") + "+" + DSL.field("g2.row") + ")*("
									+ DSL.field("g2.col") + "+" + DSL.field("g2.row") + "+1)*0.5)+"
									+ DSL.field("g2.row"), Long.class).as("target_grid_cell_id"), // same algorithm as ApiUtil.getCellId
							DSL.field("st_area(st_intersection(" + g1Name + ".geom, " + "g2.geom)) / st_area("
									+ g1Name + ".geom)", Double.class).as("percentage"))
					.from(g1Name)
					.join(
							DSL.select(
									DSL.field(g2Name + ".col").as("col"),
									DSL.field(g2Name + ".row").as("row"),
									DSL.field("st_transform(" + g2Name + ".geom, " + g1Srid + ")").as("geom"))
							.from(g2Name)
							.asTable("g2"))
					.on("st_intersects(" + g1Name + ".geom, g2.geom)")
					.where(DSL.condition("st_area(st_intersection(g2.geom, " + g1Name + ".geom)) / st_area(g2.geom) > 0.00000001"));


			// insert the forward ratios into crosswalk_entry table
			dslContext.insertInto(CROSSWALK_ENTRY)
					.columns(CROSSWALK_ENTRY.CROSSWALK_ID, CROSSWALK_ENTRY.SOURCE_COL, CROSSWALK_ENTRY.SOURCE_ROW,
							CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID, CROSSWALK_ENTRY.TARGET_COL, CROSSWALK_ENTRY.TARGET_ROW,
							CROSSWALK_ENTRY.TARGET_GRID_CELL_ID, CROSSWALK_ENTRY.PERCENTAGE)
					.select(fwQuery).execute();

		} catch (Exception e) {
			log.error("error calculating or inserting crosswalks");
			e.printStackTrace();

			if (forwardCrosswalkID != null) {
				DSL.using(JooqUtil.getJooqConfiguration())
					.deleteFrom(CROSSWALK_DATASET)
					.where(CROSSWALK_DATASET.ID.eq(forwardCrosswalkID))
					.execute();
			}

			success = false;
		}
		return success;
	}

	public static boolean ensureCrosswalkExists(Integer sourceId, Integer targetId) {
		// Look for a crosswalk that will convert from sourceId to targetId
		// If it doesn't already exist, create it now

		// TODO: When checking, need to think about the possiblity that the crosswalk is being created in another thread
		// If so, we might have the crosswalk_dataset but not the crosswalk_entry records in the db
		// TODO Pass the messages object in here so the crosswalk generator can update when a background task is running
		// and keep the runner alive

		if (sourceId == targetId) {
			log.debug("No need for crosswalk as source and target are the same: " + sourceId);
			return true;
		}

		List<Integer> gridSourceIds2010 = Arrays.asList(18, 19, 20);
		List<Integer> gridTargetIds2010 = Arrays.asList(18, 19);
		List<Integer> gridIds2020 = Arrays.asList(68, 69, 70, 83);
		if ((gridSourceIds2010.contains(sourceId) && gridIds2020.contains(targetId)) || (gridIds2020.contains(sourceId) && gridTargetIds2010.contains(targetId))) {
			log.error("Do not create crosswalks for certain grid definitions");
			return false;
		}

		DSLContext dslContext = DSL.using(JooqUtil.getJooqConfiguration());
		// Check and create source to target
		Record1<Integer> cw = dslContext.select(CROSSWALK_DATASET.ID).from(CROSSWALK_DATASET)
				.where(CROSSWALK_DATASET.SOURCE_GRID_ID.eq(sourceId).and(CROSSWALK_DATASET.TARGET_GRID_ID.eq(targetId)))
				.fetchAny();

		if (cw == null || cw.getValue(CROSSWALK_DATASET.ID) == null) {
			log.debug("Creating crosswalk for " + sourceId + " to " + targetId);
			boolean success = calculateAreaWeights(sourceId, targetId);
			if (success) {
				log.debug("Created crosswalk for " + sourceId + " to " + targetId);
			} else {
				log.error("Error creating crosswalk for " + sourceId + " to " + targetId);
				return false;
			}
		} else {
			log.debug("Crosswalk " + cw.getValue(CROSSWALK_DATASET.ID) + " exists for " + sourceId + " to " + targetId);
		}
		
		// Check and create target to source
		cw = dslContext.select(CROSSWALK_DATASET.ID).from(CROSSWALK_DATASET)
				.where(CROSSWALK_DATASET.SOURCE_GRID_ID.eq(targetId).and(CROSSWALK_DATASET.TARGET_GRID_ID.eq(sourceId)))
				.fetchAny();

		if (cw == null || cw.getValue(CROSSWALK_DATASET.ID) == null) {
			log.debug("Creating crosswalk for " + targetId + " to " + sourceId);
			boolean success = calculateAreaWeights(targetId, sourceId);
			if (success) {
				log.debug("Created crosswalk for " + targetId + " to " + sourceId);
			} else {
				log.error("Error creating crosswalk for " + targetId + " to " + sourceId);
				return false;
			}
		} else {
			log.debug("Crosswalk " + cw.getValue(CROSSWALK_DATASET.ID) + " exists for " + targetId + " to " + sourceId);
		}
		// TODO: Update this to return false if an error occurs
		return true;
	}
}