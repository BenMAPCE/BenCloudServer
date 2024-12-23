package gov.epa.bencloud.server.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import gov.epa.bencloud.server.util.ApplicationUtil;

public class PooledDataSource {

	private static HikariDataSource dataSource;
	public static String dbHost;
	public static String dbPort;
	public static String dbName;
	public static String dbUser;
	public static String dbPassword;
	private static final Logger log = LoggerFactory.getLogger(PooledDataSource.class);

	static {

		boolean databasePropertiesFound = true;
		
		String databaseHost = null;
		String databasePort = null;
		String databaseName = null;
		String databaseUser = null;
		String databasePassword = null;

		if (ApplicationUtil.usingLocalProperties()) {
			databaseHost = ApplicationUtil.getProperty("postgresql.host");
			databasePort = ApplicationUtil.getProperty("postgresql.port");
			databaseName = ApplicationUtil.getProperty("postgresql.database");
			databaseUser = ApplicationUtil.getProperty("postgresql.user");
			databasePassword = ApplicationUtil.getProperty("postgresql.password");

			if ((null == databaseHost) || 
					(null == databasePort) ||
					(null == databaseName) ||
					(null == databaseUser) ||
					(null == databasePassword)
					) {
				log.info("some or all bencloud-local.properties not found");
				databasePropertiesFound = false;
			} else {
				databasePropertiesFound = true;
				log.info("using bencloud-local.properties");
			}
		} else {

			log.info("bencloud-local.properties not found");

			databaseHost = System.getenv("DB_HOST");
			databasePort = System.getenv("DB_PORT");
			databaseName = System.getenv("DB_DATABASE");
			databaseUser = System.getenv("DB_USERNAME");
			databasePassword = System.getenv("DB_PASSWORD");

			if (null == databaseHost || null == databasePort || null == databaseName || null == databaseUser || null == databasePassword
					|| databaseHost.isEmpty() || databasePort.isEmpty() || databaseName.isEmpty() || databaseUser.isEmpty() || databasePassword.isEmpty()) {
				log.info("One or more environment variables not found");
				log.info("databaseHost: " + databaseHost);
				log.info("databasePort: " + databasePort);
				log.info("databaseName: " + databaseName);
				log.info("databaseUser: " + databaseUser);
				log.info("databasePassword: " + databasePassword);
				databasePropertiesFound = false;
			} else {
				log.info("using environment variables");
				log.info("DB_HOST: " + databaseHost);
			}
		}
		
		if (databasePropertiesFound) {
			Properties props = new Properties();
			props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
			props.setProperty("dataSource.user", databaseUser);
			props.setProperty("dataSource.password", databasePassword);
			props.setProperty("dataSource.databaseName", databaseName);
			props.setProperty("dataSource.serverName", databaseHost);
			props.setProperty("dataSource.portNumber", databasePort);
			
			//props.put("dataSource.logWriter", new PrintWriter(System.out));

			HikariConfig config = new HikariConfig(props);
			
			config.setMaximumPoolSize(25);
			config.setMinimumIdle(10);
			config.setRegisterMbeans(true);

			config.setPoolName("benmap");
			config.setAutoCommit(true);
			

			MetricRegistry metricRegistry = new MetricRegistry();
			HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
			
			final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
			reporter.start();
			
			config.setMetricRegistry(metricRegistry);
			config.setHealthCheckRegistry(healthCheckRegistry);
			
			config.addHealthCheckProperty("connectivityCheckTimeoutMs", "1000");
			config.addHealthCheckProperty("expected99thPercentileMs", "10");

			config.setConnectionTimeout(10000);
			
			try {
				dataSource = new HikariDataSource( config );
				dbHost = databaseHost;
				dbPort = databasePort;
				dbName = databaseName;
				dbUser = databaseUser;
				dbPassword = databasePassword;
			} catch (Exception e1) {
				log.error("Error creating HikariDataSource", e1);
			}
		}
	}

	public static HikariDataSource getDataSource() {
		return dataSource;
	}
	
	public static DSLContext getDSLContext() {
		return DSL.using(dataSource, SQLDialect.POSTGRES);
	}
	
	public static void shutdownDataSource() {

		try {
			Connection connection = dataSource.getConnection();
			connection.createStatement().execute("SHUTDOWN IMMEDIATELY");

			dataSource.unwrap(HikariDataSource.class).close();

		} catch (SQLException e) {
			log.error("Error shutting down data source", e);
		}
	}

}
