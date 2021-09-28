package gov.epa.bencloud.server.routes;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import gov.epa.bencloud.server.util.FreeMarkerRenderUtil;
import spark.Service;

public class PublicRoutes extends RoutesBase {

	private static final Logger log = LoggerFactory.getLogger(PublicRoutes.class);
	private Service service = null;

	public PublicRoutes(Service service, Configuration freeMarkerConfiguration){
		this.service = service;
		addRoutes(freeMarkerConfiguration);
	}

	private void addRoutes(Configuration freeMarkerConfiguration) {

		service.notFound((request, response) -> {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("page", request.pathInfo());
			return FreeMarkerRenderUtil.render(freeMarkerConfiguration, attributes, "/error/404.ftl");
		});

		service.internalServerError((request, response) -> {
			Map<String, Object> attributes = new HashMap<>();

			return FreeMarkerRenderUtil.render(freeMarkerConfiguration, attributes, "/error/500.ftl");
		});

	}
}
