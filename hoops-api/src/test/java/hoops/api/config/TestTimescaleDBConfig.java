package hoops.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@TestConfiguration
public class TestTimescaleDBConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> timescaledbContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("timescale/timescaledb:latest-pg15")
                    .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("hoopsdb_test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init/01_schema.sql")
                .withCommand("postgres -c shared_preload_libraries=timescaledb")
                .withReuse(true);
        container.start();
        
        // Execute test data after schema is loaded
        try (var conn = container.createConnection("");
             InputStream is = getClass().getClassLoader()
                 .getResourceAsStream("db/init/02_test_data.sql")) {
            
            if (is == null) {
                throw new RuntimeException("Could not find test data file");
            }
            
            String testData = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            conn.createStatement().execute(testData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data", e);
        }
        
        return container;
    }

    @Bean
    @DependsOn("timescaledbContainer")
    public DataSource dataSource(PostgreSQLContainer<?> timescaledbContainer) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        // Use the container's dynamically assigned port
        dataSource.setUrl(timescaledbContainer.getJdbcUrl());
        dataSource.setUsername(timescaledbContainer.getUsername());
        dataSource.setPassword(timescaledbContainer.getPassword());
        return dataSource;
    }

    @Bean
    @DependsOn("dataSource")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
} 