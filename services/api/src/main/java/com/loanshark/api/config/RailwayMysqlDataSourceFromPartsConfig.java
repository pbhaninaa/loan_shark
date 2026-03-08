package com.loanshark.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Creates a DataSource from Railway's separate MySQL vars when MYSQL_URL is not set.
 * On Railway BackEnd: reference MYSQLHOST, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE from your MySQL service.
 */
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnExpression("'${MYSQLHOST:}'.length() > 0 && '${MYSQL_URL:}'.length() == 0")
public class RailwayMysqlDataSourceFromPartsConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        String host = env.getProperty("MYSQLHOST");
        if (host == null) host = System.getenv("MYSQLHOST");
        String port = env.getProperty("MYSQLPORT");
        if (port == null) port = System.getenv("MYSQLPORT");
        if (port == null || port.isBlank()) port = "3306";
        String user = env.getProperty("MYSQLUSER");
        if (user == null) user = System.getenv("MYSQLUSER");
        String password = env.getProperty("MYSQLPASSWORD");
        if (password == null) password = System.getenv("MYSQLPASSWORD");
        String database = env.getProperty("MYSQLDATABASE");
        if (database == null) database = System.getenv("MYSQLDATABASE");
        if (database == null || database.isBlank()) database = "railway";

        if (host == null || host.isBlank() || user == null || password == null) {
            throw new IllegalStateException(
                "When using MYSQLHOST, set MYSQLHOST, MYSQLUSER, MYSQLPASSWORD (and optionally MYSQLPORT, MYSQLDATABASE) on the BackEnd service."
            );
        }

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=true";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(20000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }
}
