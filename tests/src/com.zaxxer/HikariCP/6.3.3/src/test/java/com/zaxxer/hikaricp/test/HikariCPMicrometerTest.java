/*
 * Copyright and related rights waived via CC0
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package com.zaxxer.hikaricp.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikaricp.test.driver.CustomDriver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class HikariCPMicrometerTest {

    @Test
    @Disabled
    void test() throws Exception {
        // Register SimpleMeterRegistry to globalRegistry before creating HikariDataSource
        MeterRegistry registry = new SimpleMeterRegistry();

        String poolName = "MyHikariPool";

        try (HikariDataSource ds = getDataSource(poolName)) {
            ds.setMetricRegistry(registry);
            // Use the connection once to trigger metrics registration
            try (Connection conn = ds.getConnection()) {
                assertNotNull(conn);
            }

            // Wait up to 1.5 seconds for metrics to appear
            Instant end = Instant.now().plus(Duration.ofSeconds(2));
            Double active = null, idle = null;
            while (Instant.now().isBefore(end)) {
                active = registry.find("hikaricp.connections.active").tag("pool", poolName).gauge() != null
                        ? registry.find("hikaricp.connections.active").tag("pool", poolName).gauge().value()
                        : null;
                idle = registry.find("hikaricp.connections.idle").tag("pool", poolName).gauge() != null
                        ? registry.find("hikaricp.connections.idle").tag("pool", poolName).gauge().value()
                        : null;
                if (active != null && idle != null) {
                    break;
                }
                Thread.sleep(50);
            }

            assertNotNull(active, "Active connection metric should be present");
            assertNotNull(idle, "Idle connection metric should be present");
            assertTrue(active >= 0, "Active connections must be non-negative");
            assertTrue(idle >= 0, "Idle connections must be non-negative");
        } finally {
            Metrics.globalRegistry.clear();
        }
    }

    private static HikariDataSource getDataSource(String poolName) {
        HikariConfig config = new HikariConfig();

        config.setPoolName(poolName);
        config.setAutoCommit(false);
        config.setConnectionTimeout(1000);
        config.setMaximumPoolSize(10);
        config.setDriverClassName(CustomDriver.class.getName());
        //config.setInitializationFailTimeout(0);
        config.setJdbcUrl("jdbc:custom:foo");
        config.setUsername("bart");
        config.setPassword("51mp50n");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }
}
