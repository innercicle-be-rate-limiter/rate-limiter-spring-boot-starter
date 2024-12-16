package com.innercircle.container;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainerInitializer {

    static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;

    static {
        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer("postgres");
        POSTGRE_SQL_CONTAINER.start();
    }
}