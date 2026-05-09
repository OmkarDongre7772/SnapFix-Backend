package com.snapfix.integration.db;

import com.snapfix.common.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresConnectivityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Testcontainers PostgreSQL starts and can be queried")
    void contextLoads_canQueryDatabase() {
        // Given / When
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("PostGIS extension is installed")
    void contextLoads_postGisIsAvailable() {
        // Given / When
        String version = jdbcTemplate.queryForObject("SELECT postgis_full_version()", String.class);

        // Then
        assertThat(version).contains("POSTGIS");
    }
}
