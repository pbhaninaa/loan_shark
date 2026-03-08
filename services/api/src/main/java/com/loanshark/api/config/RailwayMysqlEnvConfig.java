package com.loanshark.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * When MYSQL_URL is set (e.g. Railway reference like ${{ MySQL-Q-C2.MYSQL_URL }}),
 * parses it and sets SPRING_DATASOURCE_* so the app connects to that database.
 * Handles both "mysql://user:password@host:port/database" and "jdbc:mysql://..." formats.
 */
public class RailwayMysqlEnvConfig implements EnvironmentPostProcessor {

    private static final String MYSQL_URL = "MYSQL_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Prefer Spring env, then system env (Railway injects refs as env vars)
        String mysqlUrl = environment.getProperty(MYSQL_URL);
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            mysqlUrl = System.getenv(MYSQL_URL);
        }
        if (mysqlUrl == null || mysqlUrl.isBlank()) {
            // Fallback: build from Railway's separate vars (e.g. MYSQLHOST, MYSQLUSER, ...)
            String host = envOrProp(environment, "MYSQLHOST");
            String port = envOrProp(environment, "MYSQLPORT");
            String user = envOrProp(environment, "MYSQLUSER");
            String pass = envOrProp(environment, "MYSQLPASSWORD");
            String database = envOrProp(environment, "MYSQLDATABASE");
            if (host != null && user != null && pass != null && database != null) {
                String url = "jdbc:mysql://" + host + ":" + (port != null ? port : "3306") + "/" + database + "?useSSL=true";
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", url);
                props.put("spring.datasource.username", user);
                props.put("spring.datasource.password", pass);
                environment.getPropertySources().addFirst(new MapPropertySource("railwayMysql", props));
            }
            return;
        }

        Map<String, Object> props = new HashMap<>();
        if (mysqlUrl.startsWith("jdbc:mysql:")) {
            props.put("spring.datasource.url", mysqlUrl);
            // Railway may still set user/pass separately; if not, leave to user
            String user = environment.getProperty("MYSQL_USER");
            String pass = environment.getProperty("MYSQL_PASSWORD");
            if (user != null) props.put("spring.datasource.username", user);
            if (pass != null) props.put("spring.datasource.password", pass);
        } else if (mysqlUrl.startsWith("mysql://")) {
            try {
                // mysql://user:password@host:port/database
                String jdbcUrl = "jdbc:" + mysqlUrl;
                URI uri = URI.create(jdbcUrl.replace("jdbc:mysql://", "http://"));
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                String database = path != null && path.length() > 1 ? path.substring(1) : "railway";

                props.put("spring.datasource.url", "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=true");
                if (userInfo != null && !userInfo.isEmpty()) {
                    int colon = userInfo.indexOf(':');
                    if (colon > 0) {
                        props.put("spring.datasource.username", userInfo.substring(0, colon));
                        props.put("spring.datasource.password", userInfo.substring(colon + 1));
                    } else {
                        props.put("spring.datasource.username", userInfo);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Invalid " + MYSQL_URL + ": " + e.getMessage(), e);
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(
                new MapPropertySource("railwayMysql", props)
            );
        }
    }

    private static String envOrProp(ConfigurableEnvironment environment, String name) {
        String v = environment.getProperty(name);
        if (v != null && !v.isBlank()) return v;
        return System.getenv(name);
    }
}
