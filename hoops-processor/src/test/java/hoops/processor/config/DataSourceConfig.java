package hoops.processor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

@TestConfiguration
public class DataSourceConfig {
    @Bean
    @DependsOn("timescaledbContainer")
    public DataSource dataSource(PostgreSQLContainer<?> timescaledbContainer) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(timescaledbContainer.getJdbcUrl());
        dataSource.setUsername(timescaledbContainer.getUsername());
        dataSource.setPassword(timescaledbContainer.getPassword());
        return dataSource;
    }
}
