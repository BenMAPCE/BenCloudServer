package gov.epa.bencloud.server.routes;

import java.util.HashMap;
import java.util.Map;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import gov.epa.bencloud.server.database.JooqUtil;
import gov.epa.bencloud.server.util.FreeMarkerRenderUtil;
import spark.Service;

public class AdminRoutes extends RoutesBase {

	private static final Logger log = LoggerFactory.getLogger(AdminRoutes.class);
	private Service service = null;

	public AdminRoutes(Service service, Configuration freeMarkerConfiguration){
		this.service = service;
		addRoutes(freeMarkerConfiguration);
	}

	private void addRoutes(Configuration freeMarkerConfiguration) {

		service.get("/exit", (req, res) -> {
			service.stop();
			System.exit(0);
			System.out.println("shutting down....");
			return "";
		});

	}

}
