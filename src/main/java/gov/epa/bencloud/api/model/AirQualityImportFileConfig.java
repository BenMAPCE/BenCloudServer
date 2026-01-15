package gov.epa.bencloud.api.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Representation of an air quality import file configuration
 */

public class AirQualityImportFileConfig {
	private static final Logger log = LoggerFactory.getLogger(AirQualityImportFileConfig.class);
	
	public String layerName;
	public Integer filestoreId;
	public Integer aqSurfaceId;

	/*
	 * Default constructor
	 */
	public AirQualityImportFileConfig() {
		super();
	}
	
}
