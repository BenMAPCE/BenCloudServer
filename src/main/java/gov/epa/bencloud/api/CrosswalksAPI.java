package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;
import org.jooq.Field;

import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkEntryRecord;

import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jooq.DSLContext;
import org.jooq.InsertOnDuplicateStep;

import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Record6;


import org.jooq.SelectConditionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





public class CrosswalksApi{
   /**
    * calculates the crosswalks between two different grid definitions in both directions
    * @param grid_id1
    * @param grid_id2
    */
    private static final Logger log = LoggerFactory.getLogger(PollutantApi.class);

    public static void calculateCrosswalks(int grid_id1, int grid_id2){ 
        
        //add the crosswalk dataset entry into the crosswalk dataset table, then do the same but with the source and target swapped
        try{
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
            Record1<String> g1 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.TABLE_NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id1)).fetchOne();
            Record1<String> g2 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.TABLE_NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id2)).fetchOne();
        
            String g1Name = g1.value1();
            String g2Name = g2.value1();
            
            //create a table with the required fields and crosswalks for insertion into crosswalk entry table
            SelectConditionStep<Record8<Integer, Integer, Integer, Long, Integer, Integer, Long, Double>> fwQuery = dslContext
            .select(
                    DSL.value(forwardCrosswalkID).as("crosswalk_id"),
                    DSL.field(g1Name + ".col", Integer.class).as("source_col"),
                    DSL.field(g1Name + ".row", Integer.class).as("source_row"),
                    DSL.field("((" + DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + ")*(" + DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + "+1)*0.5)+" + DSL.field(g1Name + ".row"), Long.class).as("source_grid_cell_id"), //same algorithm as ApiUtil.getCellId
                    DSL.field(g2Name + ".col", Integer.class).as("target_col"),
                    DSL.field(g2Name + ".row", Integer.class).as("target_row"),
                    DSL.field("((" + DSL.field(g2Name + ".col") + "+" + DSL.field(g2Name + ".row") + ")*(" + DSL.field(g2Name + ".col") + "+" + DSL.field(g2Name + ".row") + "+1)*0.5)+" + DSL.field(g2Name + ".row"), Long.class).as("target_grid_cell_id"), //same algorithm as ApiUtil.getCellId
                    DSL.field("st_area(st_intersection(" + g1Name + ".geom, " + g2Name + ".geom)) / st_area(" + g1Name + ".geom)", Double.class).as("percentage")
            )
            .from(g1Name)
            .join(g2Name)
            .on("st_intersects(" + g1Name + ".geom," + g2Name + ".geom)")
            .where(DSL.condition("st_area(st_intersection(" + g2Name + ".geom, " + g1Name + ".geom)) / st_area(" + g2Name + ".geom) > 0.00000001"));

            //do the same but for the backwards ratios
            SelectConditionStep<Record8<Integer, Integer, Integer, Long, Integer, Integer, Long, Double>> bwQuery = dslContext
            .select(
                    DSL.value(backwardCrosswalkID).as("crosswalk_id"),
                    DSL.field(g2Name + ".col", Integer.class).as("source_col"),
                    DSL.field(g2Name + ".row", Integer.class).as("source_row"),
                    DSL.field("((" + DSL.field(g2Name + ".col") + "+" + DSL.field(g2Name + ".row") + ")*(" + DSL.field(g2Name + ".col") + "+" + DSL.field(g2Name + ".row") + "+1)*0.5)+" + DSL.field(g2Name + ".row"), Long.class).as("source_grid_cell_id"), //same algorithm as ApiUtil.getCellId
                    DSL.field(g1Name + ".col", Integer.class).as("target_col"),
                    DSL.field(g1Name + ".row", Integer.class).as("target_row"),
                    DSL.field("((" + DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + ")*(" + DSL.field(g1Name + ".col") + "+" + DSL.field(g1Name + ".row") + "+1)*0.5)+" + DSL.field(g1Name + ".row"), Long.class).as("target_grid_cell_id"), //same algorithm as ApiUtil.getCellId
                    DSL.field("st_area(st_intersection(" + g2Name + ".geom, " + g1Name + ".geom)) / st_area(" + g2Name + ".geom)", Double.class).as("percentage")
            )
            .from(g2Name)
            .join(g1Name)
            .on(DSL.condition("st_intersects(" + g2Name + ".geom, " + g1Name + ".geom)"))
            .where(DSL.condition("st_area(st_intersection(" + g2Name + ".geom, " + g1Name + ".geom)) / st_area(" + g2Name + ".geom) > 0.00000001"));
           


                //insert the forward ratios into crosswalk_entry table
                dslContext.insertInto(CROSSWALK_ENTRY)
                .columns(
                    CROSSWALK_ENTRY.CROSSWALK_ID,
                    CROSSWALK_ENTRY.SOURCE_COL,
                    CROSSWALK_ENTRY.SOURCE_ROW,
                    CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID,
                    CROSSWALK_ENTRY.TARGET_COL,
                    CROSSWALK_ENTRY.TARGET_ROW,
                    CROSSWALK_ENTRY.TARGET_GRID_CELL_ID,
                    CROSSWALK_ENTRY.PERCENTAGE
                )
                .select(fwQuery)
                .execute();
                
            
                //insert the backwards ratios
                dslContext.insertInto(CROSSWALK_ENTRY)
                .columns(
                    CROSSWALK_ENTRY.CROSSWALK_ID,
                    CROSSWALK_ENTRY.SOURCE_COL,
                    CROSSWALK_ENTRY.SOURCE_ROW,
                    CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID,
                    CROSSWALK_ENTRY.TARGET_COL,
                    CROSSWALK_ENTRY.TARGET_ROW,
                    CROSSWALK_ENTRY.TARGET_GRID_CELL_ID,
                    CROSSWALK_ENTRY.PERCENTAGE
                )
                .select(bwQuery)
                .execute();

            // long testCellId = ApiUtil.getCellId(48, 501);
            // System.out.println(testCellId);
        }
        catch (Exception e){
            log.error("error calculating or inserting crosswalks");
            e.printStackTrace();
        }
                     
    }
    

   

    // private static long getGridCellId(Field<Integer> col, Field<Integer> row) {
    //     int int_col = col.cast(Integer.class).getValue();
    //     int int_row = row.cast(Integer.class).getValue();
    //     return ApiUtil.getCellId(int_col, int_row);
    // }
    // public static void main(String[] args) {
    //     // Call your calculateCrosswalks method here to test
    //     int grid_id1 = 18; // Replace with your desired grid IDs
    //     int grid_id2 = 19;
    //     calculateCrosswalks(grid_id1, grid_id2);

    //     System.out.println("Crosswalk calculation completed.");
    // }

}