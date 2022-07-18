package gov.epa.bencloud.server.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

/*
 * Methods used for configuring and rendering freemarker resources.
 */
public class FreeMarkerRenderUtil {
	private static final Logger log = LoggerFactory.getLogger(FreeMarkerRenderUtil.class);

	/**
	 * Creates a new file from the template path and sets the freemarker configuration parameters. 
	 * @param templatePath
	 * @return a Configuration object with the proper freemarker configuration parameters.
	 */
	public static Configuration configureFreemarker(String templatePath) {
		
		Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_31);
		
		try {
			FileTemplateLoader templateLoader = 
					new FileTemplateLoader(new File(templatePath));

			freeMarkerConfiguration.setDefaultEncoding("UTF-8");
			freeMarkerConfiguration.setTemplateExceptionHandler(
					TemplateExceptionHandler.RETHROW_HANDLER);

			freeMarkerConfiguration.setTemplateLoader(templateLoader);
			
		} catch (IOException e) {
			log.error("Error configuring freemarker", e);
		}
		return freeMarkerConfiguration;
		
	}
	
	/**
	 * 
	 * @param freeMarkerConfiguration
	 * @param model
	 * @param templatePath
	 * @return renders the model and view from the given model and template path.
	 */
	public static String render(
			Configuration freeMarkerConfiguration, 
			Map<String, Object> model, 
			String templatePath) {
		
	    return new FreeMarkerEngine(freeMarkerConfiguration)
	    		.render(new ModelAndView(model, templatePath));
	}


	public static void logPath(
			HttpServletRequest httpServletRequest, 
			HttpServletResponse httpServletResponse) {
	}

}
