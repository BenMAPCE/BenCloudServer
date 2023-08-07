package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkEntryRecord;

import org.jooq.impl.DSL;
import org.jooq.DSLContext;
import org.jooq.InsertOnDuplicateStep;

import org.jooq.Record1;
import org.jooq.Record7;

import org.jooq.SelectConditionStep;




public class CrosswalksAPI{
   /**
    * calculates the crosswalks between two different grid definitions in both directions
    * @param grid_id1
    * @param grid_id2
    */
    public static void calculateCrosswalks(int grid_id1, int grid_id2){ 
        
        //add the crosswalk dataset entry into the crosswalk dataset table, then do the same but with the source and target swapped
        DSLContext dslContext = DSL.using(JooqUtil.getJooqConfiguration());
        CrosswalkDatasetRecord fwCrosswalkDatasetRecord = dslContext
			.insertInto(CROSSWALK_DATASET
					, CROSSWALK_DATASET.SOURCE_GRID_ID
					, CROSSWALK_DATASET.TARGET_GRID_ID
            )
			.values(grid_id2,  grid_id1)
			.returning(CROSSWALK_DATASET.ID)
			.fetchOne();

        CrosswalkDatasetRecord bwCrosswalkDatasetRecord = dslContext
			.insertInto(CROSSWALK_DATASET
					, CROSSWALK_DATASET.SOURCE_GRID_ID
					, CROSSWALK_DATASET.TARGET_GRID_ID
            )
			.values(grid_id1,  grid_id2)
			.returning(CROSSWALK_DATASET.ID)
			.fetchOne();

        //store the ids
        Integer forwardCrosswalkID = fwCrosswalkDatasetRecord.value1();
        Integer backwardCrosswalkID = bwCrosswalkDatasetRecord.value1();


        //get the grid names from the grid definition table for the given grid ids
        Record1<String> g1 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id1)).fetchOne();
        Record1<String> g2 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id2)).fetchOne();
    
        String g1Name = g1.value1();
        String g2Name = g2.value1();

        //create a table with the required fields and crosswalks and insert into crosswalk entry table
        SelectConditionStep<Record7<Integer, Integer, Long, Integer, Integer, Long, Double>> selectQuery = dslContext
        .select(
                DSL.field(g1Name + ".col", Integer.class).as("source_col"),
                DSL.field(g1Name + ".row", Integer.class).as("source_row"),
                DSL.field(g1Name + ".grid_cell_id", Long.class).as("source_grid_cell_id"),
                DSL.field(g2Name + ".col", Integer.class).as("target_col"),
                DSL.field(g2Name + ".row", Integer.class).as("target_row"),
                DSL.field(g2Name + ".grid_cell_id", Long.class).as("target_grid_cell_id"),
                DSL.field("st_area(st_intersection(" + g1Name + ".geom, " + g2Name + ".geom)) / st_area(" + g1Name + ".geom)", Double.class).as("percentage")
        )
        .from(g1Name, g2Name)
        .where("st_intersects(g1.geom, g2.geom)")
        .and("st_area(st_intersection(g1.geom, g2.geom)) / st_area(g1.geom) > 0.00000001");

        dslContext.insertInto(CROSSWALK_ENTRY,
                CROSSWALK_ENTRY.CROSSWALK_ID,
                CROSSWALK_ENTRY.SOURCE_COL,
                CROSSWALK_ENTRY.SOURCE_ROW,
                CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID,
                CROSSWALK_ENTRY.TARGET_COL,
                CROSSWALK_ENTRY.TARGET_ROW,
                CROSSWALK_ENTRY.TARGET_GRID_CELL_ID,
                CROSSWALK_ENTRY.PERCENTAGE)
        .select(DSL.select(
                DSL.value(forwardCrosswalkID),
                selectQuery.field("source_col", Integer.class),
                selectQuery.field("source_row", Integer.class),
                selectQuery.field("source_grid_cell_id", Long.class),
                selectQuery.field("target_col", Integer.class),
                selectQuery.field("target_row", Integer.class),
                selectQuery.field("target_grid_cell_id", Long.class),
                selectQuery.field("percentage", Double.class)
            ));
    

        //do the same but for the backwards ratios
        SelectConditionStep<Record7<Integer, Integer, Long, Integer, Integer, Long, Double>> bwSelectQuery = dslContext
        .select(
                DSL.field(g2Name + ".col", Integer.class).as("source_col"),
                DSL.field(g2Name + ".row", Integer.class).as("source_row"),
                DSL.field(g2Name + ".grid_cell_id", Long.class).as("source_grid_cell_id"),
                DSL.field(g1Name + ".col", Integer.class).as("target_col"),
                DSL.field(g1Name + ".row", Integer.class).as("target_row"),
                DSL.field(g1Name + ".grid_cell_id", Long.class).as("target_grid_cell_id"),
                DSL.field("st_area(st_intersection(" + g2Name + ".geom, " + g1Name + ".geom)) / st_area(" + g2Name + ".geom)", Double.class).as("percentage")
        )
        .from(g1Name, g2Name)
        .where("st_intersects(g2.geom, g1.geom)")
        .and("st_area(st_intersection(g1.geom, g2.geom)) / st_area(g2.geom) > 0.00000001");


        dslContext.insertInto(CROSSWALK_ENTRY,
                CROSSWALK_ENTRY.CROSSWALK_ID,
                CROSSWALK_ENTRY.SOURCE_COL,
                CROSSWALK_ENTRY.SOURCE_ROW,
                CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID,
                CROSSWALK_ENTRY.TARGET_COL,
                CROSSWALK_ENTRY.TARGET_ROW,
                CROSSWALK_ENTRY.TARGET_GRID_CELL_ID,
                CROSSWALK_ENTRY.PERCENTAGE)
        .select(DSL.select(
                DSL.value(backwardCrosswalkID),
                bwSelectQuery.field("source_col", Integer.class),
                bwSelectQuery.field("source_row", Integer.class),
                bwSelectQuery.field("source_grid_cell_id", Long.class),
                bwSelectQuery.field("target_col", Integer.class),
                bwSelectQuery.field("target_row", Integer.class),
                bwSelectQuery.field("target_grid_cell_id", Long.class),
                bwSelectQuery.field("percentage", Double.class)
            ));
    
       
                     
    }
}