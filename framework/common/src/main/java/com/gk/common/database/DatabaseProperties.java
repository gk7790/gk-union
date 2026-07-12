package com.gk.common.database;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Framework database selection. */
@ConfigurationProperties(prefix = "gk.datasource")
public class DatabaseProperties {
    private DatabaseType type = DatabaseType.MYSQL;

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }
}
