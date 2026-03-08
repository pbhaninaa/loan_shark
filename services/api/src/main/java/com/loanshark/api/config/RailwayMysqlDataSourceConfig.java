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
import java.net.URI;

/**
 * Fallback: creates a DataSource from MYSQL_* when RailwayMysqlEnvConfig did not set spring.datasource.*
 * (e.g. no MYSQL_URL/MYSQL_PUBLIC_URL at env post-process time). When those are set, the env
 * post-processor sets spring.datasource.* and Spring's DataSource auto-config is used instead.
 */
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnExpression("'${spring.datasource.url:}'.isEmpty() && (!'${MYSQL_URL:}'.isEmpty() || !'${MYSQL_PUBLIC_URL:}'.isEmpty())")
public class RailwayMysqlDataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        // Prefer MYSQL_PUBLIC_URL so connection works when private host (*.railway.internal) does not resolve.
        String mysqlUrl = firstNonBlank(
            env.getProperty("MYSQL_PUBLIC_URL"),
            System.getenv("MYSQL_PUBLIC_URL"),
            env.getProperty("MYSQL_URL"),
            System.getenv("MYSQL_URL")
        );
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            throw new IllegalStateException("MYSQL_URL (or MYSQL_PUBLIC_URL) must be set on the BackEnd service. In Railway: BackEnd → Variables → MYSQL_URL = ${{ YourMySQLService.MYSQL_URL }}");
        }
        // Private host only resolves inside Railway's network; use public URL so the app can connect.
        if (mysqlUrl.contains("railway.internal")) {
            String publicUrl = firstNonBlank(
                env.getProperty("MYSQL_PUBLIC_URL"),
                System.getenv("MYSQL_PUBLIC_URL")
            );
            if (publicUrl == null || publicUrl.isBlank()) {
                throw new IllegalStateException(
                    "MYSQL_URL points to Railway's private host (railway.internal), which is not reachable from here. "
                    + "On Railway: BackEnd → Variables → add MYSQL_PUBLIC_URL = ${{ YourMySQLService.MYSQL_PUBLIC_URL }}");
            }
            mysqlUrl = publicUrl;
        }

        String jdbcUrl;
        String username;
        String password;

        if (mysqlUrl.startsWith("jdbc:mysql:")) {
            jdbcUrl = mysqlUrl;
            username = firstNonBlank(env.getProperty("MYSQLUSER"), System.getenv("MYSQLUSER"));
            password = firstNonBlank(env.getProperty("MYSQLPASSWORD"), System.getenv("MYSQLPASSWORD"));
        } else if (mysqlUrl.startsWith("mysql://")) {
            URI uri = URI.create(mysqlUrl.replace("mysql://", "http://"));
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath();
            String database = path != null && path.length() > 1 ? path.substring(1) : "railway";
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=true";
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                int colon = userInfo.indexOf(':');
                username = colon > 0 ? userInfo.substring(0, colon) : userInfo;
                password = colon > 0 ? userInfo.substring(colon + 1) : "";
            } else {
                username = firstNonBlank(env.getProperty("MYSQLUSER"), System.getenv("MYSQLUSER"));
                password = firstNonBlank(env.getProperty("MYSQLPASSWORD"), System.getenv("MYSQLPASSWORD"));
            }
        } else {
            throw new IllegalStateException("MYSQL_URL must start with mysql:// or jdbc:mysql://");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password != null ? password : "");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(20000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
