package gov.epa.bencloud.server.database;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.tools.jdbc.DefaultConnection;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.jooq.impl.DefaultConfiguration;

public class AppNameConnectionProvider extends DataSourceConnectionProvider {
    private final String appName;

    public AppNameConnectionProvider(DataSource dataSource, String appName) {
        super(dataSource);
        this.appName = appName;
    }

    @Override
    public Connection acquire() {
        Connection conn = super.acquire();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET application_name = '" + appName + "'");
        } catch (SQLException e) {
            throw new RuntimeException("Error setting application_name", e);
        }
        return conn;
    }

}
