/*
 * This file is generated by jOOQ.
 */
package gov.epa.bencloud.server.database.jooq.grids;


import gov.epa.bencloud.server.database.jooq.grids.tables.UsCmaq_12kmNation;
import gov.epa.bencloud.server.database.jooq.grids.tables.UsCmaq_12kmNationClipped;
import gov.epa.bencloud.server.database.jooq.grids.tables.UsCounty;
import gov.epa.bencloud.server.database.jooq.grids.tables.UsNation;
import gov.epa.bencloud.server.database.jooq.grids.tables.UsState;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in grids.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index US_CMAQ_12KM_NATION_CLIPPED_GEOM_IDX = Internal.createIndex(DSL.name("us_cmaq_12km_nation_clipped_geom_idx"), UsCmaq_12kmNationClipped.US_CMAQ_12KM_NATION_CLIPPED, new OrderField[] { UsCmaq_12kmNationClipped.US_CMAQ_12KM_NATION_CLIPPED.GEOM }, false);
    public static final Index US_CMAQ_12KM_NATION_GEOM_IDX = Internal.createIndex(DSL.name("us_cmaq_12km_nation_geom_idx"), UsCmaq_12kmNation.US_CMAQ_12KM_NATION, new OrderField[] { UsCmaq_12kmNation.US_CMAQ_12KM_NATION.GEOM }, false);
    public static final Index US_COUNTY_GEOM_IDX = Internal.createIndex(DSL.name("us_county_geom_idx"), UsCounty.US_COUNTY, new OrderField[] { UsCounty.US_COUNTY.GEOM }, false);
    public static final Index US_NATION_GEOM_IDX = Internal.createIndex(DSL.name("us_nation_geom_idx"), UsNation.US_NATION, new OrderField[] { UsNation.US_NATION.GEOM }, false);
    public static final Index US_STATE_GEOM_IDX = Internal.createIndex(DSL.name("us_state_geom_idx"), UsState.US_STATE, new OrderField[] { UsState.US_STATE.GEOM }, false);
}
