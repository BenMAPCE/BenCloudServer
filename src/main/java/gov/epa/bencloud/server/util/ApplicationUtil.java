package gov.epa.bencloud.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import gov.epa.bencloud.server.BenCloudServer;


/*
 * 
 */
public class ApplicationUtil {
	private static final Logger log = LoggerFactory.getLogger(ApplicationUtil.class);
	
	public static Properties properties = new Properties();

	/**
	 * 
	 * @return true if the local properties file exists, false if not.
	 */
	public static boolean usingLocalProperties() {

		try {
			String applicationPath = new File(".").getCanonicalPath();
					
			String propertiesPath = applicationPath + File.separator + "bencloud-local.properties";
			File propertiesFile = new File(propertiesPath);

			if (propertiesFile.exists()) {
				return true;
			}
		} catch (IOException e) {
			log.error("Unable to access bencloud-local.properties", e);
		}
		
		return false;
	}
	
	/**
	 * Loads the given properties file, if it exists.
	 * @param propertiesFileName
	 * @throws IOException
	 */
	public static void loadProperties(String propertiesFileName) throws IOException {
		loadProperties(propertiesFileName, false);
	}
	
	/**
	 * Loads the given properties file, if it exists.
	 * @param propertiesFileName
	 * @param optional
	 * @throws IOException
	 */
	public static void loadProperties(
			String propertiesFileName, boolean optional) throws IOException {

		String applicationPath = new File(".").getCanonicalPath();
				
		String propertiesPath = applicationPath + File.separator + propertiesFileName;
		File propertiesFile = new File(propertiesPath);

		if (!propertiesFile.exists()) {
			if (!optional) {
				System.out.println("Sorry, unable to find " + propertiesFileName);
				return;
			}
		} else {
			try (InputStream input = new FileInputStream(propertiesFile)) {

				properties.load(input);
				
			} catch (IOException e) {
				log.error("Unable to local bencloud-local.properties", e);
			}
		}
	}

	/**
	 * 
	 * @param property
	 * @return a value of a given property. 
	 */
	public static String getProperty(String property) {

		return properties.getProperty(property);
	}

	/**
	 * Sets the value of a given property.
	 * @param propertyName
	 * @param propertyValue
	 */
	private static void setProperty(String propertyName, String propertyValue) {

		properties.setProperty(propertyName, propertyValue);
		return;
	}

	/**
	 * 
	 * @return true if the value of max.task.workers > 0, false if null or if <= 0.
	 * @throws IOException
	 */
	public static boolean validateProperties() throws IOException {

		boolean propertiesOK = true;

//		if (!checkPropertyDirectory("config.directory")) {
//			propertiesOK = false;
//		}

		if (null == getProperty("max.task.workers")) {
			System.out.println("max.task.workers property not defined");
			propertiesOK = false;
		} else {
			int maxTaskWorkers = 0;
			
			try {
				maxTaskWorkers = Integer.parseInt(getProperty("max.task.workers"));
				if (maxTaskWorkers <= 0) {
					System.out.println("max.task.workers must be > 0");
				}
			} catch (NumberFormatException e) {
				log.error("max.task.workers property is not numeric", e);
			}
		}

		return propertiesOK;
	}

	/**
	 * Validates the value of a specific property.
	 * @param propertyName
	 * @return true if 
	 * @throws IOException
	 */
	private static boolean checkPropertyDirectory(String propertyName) throws IOException {

		boolean propertyOK = true;

		String directory = getProperty(propertyName);
		if (null == directory) {
			System.out.println(propertyName + " property not defined");
			propertyOK = false;
		} else {
			
			if (directory.startsWith("../")) {
				String applicationPath = new File(".").getCanonicalPath();
				directory = directory.replace("../", applicationPath + File.separator);
				setProperty(propertyName, directory);
			}
			
			if (!new File(directory).exists()) {
				System.out.println(directory + " does not exist");
				propertyOK = false;
			}
		}

		return propertyOK;
	}	
	
	/**
	 * 
	 */
	public static void configureLogging() {

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default 
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset(); 
			configurator.doConfigure(new File(".").getCanonicalPath() + getProperty("config.directory") + File.separator + "logback.xml");
		} catch (JoranException je) {
			// StatusPrinter will handle this
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("Exception configuring logging", e);
		}

		// uncomment to check log configuration
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);

	}

	/**
	 * 
	 * @return a properties object.
	 */
	public static Properties getProperties() {
		return properties;
	}

	/**
	 * Replaces special characters in a given string
	 * @param inputString
	 * @return a cleaned string.
	 */
	public static String replaceNonValidCharacters(String inputString) {
		
		String outputString = inputString;
		outputString = outputString.replaceAll(" ", "_");
		outputString = outputString.replaceAll("[^a-zA-Z0-9\\_\\-\\s]", "_");
		return outputString;
	}
	
	/**
	 * Checks the queue directory path, creates the directory if it does not exist.
	 * @return the queue directory path.
	 */
	public static String createQueueDirectoryIfNecessary() {
		
		String queueDirectory = BenCloudServer.getApplicationPath() + 
				ApplicationUtil.getProperty("queue.directory");

		if (!new File(queueDirectory).exists()) {
			new File(queueDirectory).mkdirs();
		}

		return queueDirectory;
	}

	/**
	 * Checks the output directory path, creates the directory if it does not exist.
	 * @return the output directory path.
	 */
	public static String createOutputDirectoryIfNecessary() {
		
		String outputDirectory = BenCloudServer.getApplicationPath() + 
				ApplicationUtil.getProperty("output.directory");
				
		if (!new File(outputDirectory).exists()) {
			new File(outputDirectory).mkdirs();
		}
		
		return outputDirectory;
	}

	public static String getCurrentLocalDateTimeStamp() {
	    return LocalDateTime.now()
	       .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}
}
