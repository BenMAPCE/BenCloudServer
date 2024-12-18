package gov.epa.bencloud.server.database;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;

import gov.epa.bencloud.server.database.listeners.SQLPrinter;

public class JooqUtil {
	
	public static Configuration getJooqConfiguration() {

		Configuration configuration = new DefaultConfiguration()
				//.set(new DefaultExecuteListenerProvider(new SQLPrinter()))
			    .set(PooledDataSource.getDataSource())
			    .set(SQLDialect.POSTGRES);

		return configuration;
	}

	public static Configuration getJooqConfigurationUnquoted() {
		
		Settings settings = new Settings();
		settings.setRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);

		return getJooqConfiguration().set(settings);
	}

}
