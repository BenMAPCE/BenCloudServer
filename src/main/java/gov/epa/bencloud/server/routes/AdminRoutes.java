package gov.epa.bencloud.server.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
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
