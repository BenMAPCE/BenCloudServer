package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;
import static gov.epa.bencloud.server.database.jooq.grids.Tables.*;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkDatasetRecord;
import gov.epa.bencloud.server.database.jooq.data.tables.records.CrosswalkEntryRecord;
import org.jooq.impl.DSL;
import org.jooq.DSLContext;
import org.jooq.InsertOnDuplicateStep;
import org.jooq.InsertValuesStep7;
import org.jooq.InsertValuesStep8;

import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record7;

import org.jooq.SelectConditionStep;




public class CrosswalksAPI{
    
    public static void calculateCrosswalks(int grid_id1, int grid_id2){ 
        
        DSLContext dslContext = DSL.using(JooqUtil.getJooqConfiguration());
        CrosswalkDatasetRecord crosswalkDatasetRecord = dslContext
			.insertInto(CROSSWALK_DATASET
					, CROSSWALK_DATASET.SOURCE_GRID_ID
					, CROSSWALK_DATASET.TARGET_GRID_ID
            )
			.values(grid_id1,  grid_id2)
			.returning(CROSSWALK_DATASET.ID)
			.fetchOne();

        Integer forwardCrosswalkID = crosswalkDatasetRecord.value1();

        Record1<String> g1 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id1)).fetchOne();
        Record1<String> g2 = DSL.using(JooqUtil.getJooqConfiguration()).select(GRID_DEFINITION.NAME).from(GRID_DEFINITION).where(GRID_DEFINITION.ID.eq(grid_id2)).fetchOne();
    
        String g1Name = g1.value1();
        String g2Name = g2.value1();

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

        InsertOnDuplicateStep<CrosswalkEntryRecord> crosswalkEntryRecord = dslContext
            .insertInto(CROSSWALK_ENTRY
                    , CROSSWALK_ENTRY.CROSSWALK_ID
                    , CROSSWALK_ENTRY.SOURCE_COL
                    , CROSSWALK_ENTRY.SOURCE_ROW
                    , CROSSWALK_ENTRY.SOURCE_GRID_CELL_ID
                    , CROSSWALK_ENTRY.TARGET_COL
                    , CROSSWALK_ENTRY.TARGET_ROW
                    , CROSSWALK_ENTRY.TARGET_GRID_CELL_ID
                    , CROSSWALK_ENTRY.PERCENTAGE
            )
            .select(
            DSL.value(forwardCrosswalkID),
            selectQuery.field("source_col"),
            selectQuery.field("source_row"),
            selectQuery.field("source_grid_cell_id"),
            selectQuery.field("target_col"),
            selectQuery.field("target_row"),
            selectQuery.field("target_grid_cell_id"),
            selectQuery.field("percentage")
        )
        .from(selectQuery);
            
                     
    }
}