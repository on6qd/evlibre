package com.evlibre.server.adapter.persistence.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class H2DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(H2DatabaseManager.class);

    private final HikariDataSource dataSource;

    public H2DatabaseManager(String jdbcUrl, String username, String password,
                              int poolSize, boolean runMigrations) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);

        this.dataSource = new HikariDataSource(config);

        if (runMigrations) {
            runMigrations();
        }
    }

    private void runMigrations() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        var result = flyway.migrate();
        log.info("Flyway migration: {} migrations applied", result.migrationsExecuted);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        dataSource.close();
    }
}
