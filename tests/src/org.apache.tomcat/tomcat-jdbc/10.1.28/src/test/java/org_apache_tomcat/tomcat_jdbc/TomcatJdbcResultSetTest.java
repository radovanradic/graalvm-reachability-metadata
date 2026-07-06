/*
 * Copyright and related rights waived via CC0
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package org_apache_tomcat.tomcat_jdbc;

import org.apache.tomcat.jdbc.pool.DataSourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TomcatJdbcResultSetTest {

    private DataSource dataSource;

    @BeforeAll
    void init() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.h2.Driver");
        properties.setProperty("url", "jdbc:h2:mem:result_set_pool;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.setProperty("username", "fred");
        properties.setProperty("password", "secret");
        properties.setProperty("initialSize", "2");
        properties.setProperty("minIdle", "1");
        properties.setProperty("testOnBorrow", "true");
        properties.setProperty("testOnConnect", "true");
        properties.setProperty("validationQuery", "select 1");
        dataSource = new DataSourceFactory().createDataSource(properties);
    }

    @AfterAll
    void close() throws Exception {
        ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).close(true);
    }

    @Test
    void pooledPreparedStatementCreatesResultSetProxy() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP TABLE IF EXISTS decorated_item");
                statement.executeUpdate("CREATE TABLE decorated_item (id INT NOT NULL PRIMARY KEY, name VARCHAR(255))");
                statement.executeUpdate("INSERT INTO decorated_item(id, name) VALUES (1, 'proxy-result-set')");
            }

            try (PreparedStatement preparedStatement =
                         connection.prepareStatement("SELECT name FROM decorated_item WHERE id = ?");
                 ResultSet resultSet = executeQuery(preparedStatement)) {
                assertThat(Proxy.isProxyClass(resultSet.getClass())).isTrue();
                assertThat(resultSet.getStatement()).isNotNull();
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("proxy-result-set");
            }
        }
    }

    private ResultSet executeQuery(PreparedStatement preparedStatement) throws Exception {
        preparedStatement.setInt(1, 1);
        return preparedStatement.executeQuery();
    }
}
