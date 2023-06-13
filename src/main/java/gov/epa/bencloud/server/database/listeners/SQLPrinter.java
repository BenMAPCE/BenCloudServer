package gov.epa.bencloud.server.database.listeners;

import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.tasks.TaskManager;

public class SQLPrinter extends DefaultExecuteListener {

	private static final Logger log = LoggerFactory.getLogger("org.jooq");
	
    /**
     * Hook into the query execution lifecycle before executing queries
     */
    @Override
    public void executeStart(ExecuteContext ctx) {

        // Create a new DSLContext for logging rendering purposes
        // This DSLContext doesn't need a connection, only the SQLDialect...
        DSLContext create = DSL.using(ctx.dialect(),
        
        // ... and the flag for pretty-printing
        new Settings().withRenderFormatted(true));

        
        // If we're executing a query (skip the polling of the task_worker table to reduce noise)
        if (ctx.query() != null && !ctx.query().getSQL().contains("task_worker") && !ctx.query().getSQL().contains("task_queue") && !ctx.query().getSQL().contains("task_complete")) {
            log.info(create.renderInlined(ctx.query()));
        }
        
        // If we're executing a routine
        else if (ctx.routine() != null) {
            log.info(create.renderInlined(ctx.routine()));
        }
        
    }
}