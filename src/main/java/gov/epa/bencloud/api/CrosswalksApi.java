package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkDatasetRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
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
							DSL.field("((" + DSL.field(g1Name + ".col").cast(Long.class) + "+" + DSL.field(g1Name + ".row").cast(Long.class) + ")*("
									+ DSL.field(g1Name + ".col").cast(Long.class) + "+" + DSL.field(g1Name + ".row").cast(Long.class) + "+1)*0.5)+"
									+ DSL.field(g1Name + ".row").cast(Long.class), Long.class).as("source_grid_cell_id"), // same algorithm as ApiUtil.getCellId
							DSL.field("g2.col", Integer.class).as("target_col"),
							DSL.field("g2.row", Integer.class).as("target_row"),
							DSL.field("((" + DSL.field("g2.col").cast(Long.class) + "+" + DSL.field("g2.row").cast(Long.class) + ")*("
									+ DSL.field("g2.col").cast(Long.class) + "+" + DSL.field("g2.row").cast(Long.class) + "+1)*0.5)+"
									+ DSL.field("g2.row").cast(Long.class), Long.class).as("target_grid_cell_id"), // same algorithm as ApiUtil.getCellId
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

			dslContext.update(CROSSWALK_DATASET)
					.set(CROSSWALK_DATASET.CREATED_DATE,LocalDateTime.now())
					.where(CROSSWALK_DATASET.ID.eq(forwardCrosswalkID))
					.execute();

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

	private static boolean checkAndCreateCrosswalk(Integer sourceId, Integer targetId) {

		DSLContext dslContext = DSL.using(JooqUtil.getJooqConfiguration());
		Record2<Integer,LocalDateTime> cw = dslContext.select(CROSSWALK_DATASET.ID,CROSSWALK_DATASET.CREATED_DATE).from(CROSSWALK_DATASET)
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
			final Integer crosswalkId = cw.getValue(CROSSWALK_DATASET.ID);
			if (cw.getValue(CROSSWALK_DATASET.CREATED_DATE) == null) {
				log.debug("Found existing crosswalk, but it is not completed being created.");
				final int sleepSeconds = 30;
				final int sleepTries = 60;
				LocalDateTime createdDate = null;
				for (int tryCount = 1; tryCount <= sleepTries; tryCount++) {
					log.debug("Sleeping for "+sleepSeconds+" seconds. Try #"+tryCount);
					try {
						Thread.sleep(sleepSeconds * 1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); // Re-interrupt the thread
						log.debug("Thread was interrupted while sleeping.");
						e.printStackTrace();
					}
					createdDate = dslContext.select(CROSSWALK_DATASET.CREATED_DATE)
							.from(CROSSWALK_DATASET)
							.where(CROSSWALK_DATASET.ID.eq(crosswalkId))
							.fetchAny()
							.value1();
					if (createdDate != null) {
						log.debug("Existing crosswalk (id:"+crosswalkId+") creation completed.");
						break;
					}
				}
				if (createdDate == null) {
					log.error("Timeout waiting "+sleepSeconds*sleepTries+" seconds for existing crosswalk (id:"+crosswalkId+") to complete creation.");
					return false;
				}
			}

			log.debug("Crosswalk " + crosswalkId + " exists for " + sourceId + " to " + targetId);
		}

		return true;
	}

	public static boolean ensureCrosswalkExists(Integer sourceId, Integer targetId) {
		// Look for a crosswalk that will convert from sourceId to targetId
		// If it doesn't already exist, create it now

		// TODO Pass the messages object in here so the crosswalk generator can update when a background task is running
		// and keep the runner alive

		if (sourceId == targetId) {
			log.debug("No need for crosswalk as source and target are the same: " + sourceId);
			return true;
		}
		if (targetId==0) {
			log.debug("No need to create crosswalk for targetId 0.");
			return true;
		}

		List<Integer> gridSourceIds2010 = Arrays.asList(18, 19, 20);
		List<Integer> gridTargetIds2010 = Arrays.asList(18, 19);
		List<Integer> gridIds2020 = Arrays.asList(68, 69, 70, 83);
		if ((gridSourceIds2010.contains(sourceId) && gridIds2020.contains(targetId)) || (gridIds2020.contains(sourceId) && gridTargetIds2010.contains(targetId))) {
			log.error("Do not create crosswalks for certain grid definitions");
			return false;
		}
		
		// Check and create source to target
		boolean success = checkAndCreateCrosswalk(sourceId, targetId);
		if (!success) {
			return false;
		}

		// Check and create target to source
		success = checkAndCreateCrosswalk(targetId, sourceId);
		if (!success) {
			return false;
		}

		return true;
	}
}