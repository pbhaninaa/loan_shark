package com.loanshark.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * When Railway MySQL env vars are set, parses them and sets spring.datasource.*
 * so Spring's DataSource auto-config picks them up (same as the working backend using SPRING_DATASOURCE_*).
 * Prefers MYSQL_PUBLIC_URL over MYSQL_URL so the private host (railway.internal) is never used.
 * Handles "mysql://user:password@host:port/database" and "jdbc:mysql://..." formats.
 */
public class RailwayMysqlEnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Prefer public URL so connection works when private host does not resolve
        String mysqlUrl = firstNonBlank(
            environment.getProperty("MYSQL_PUBLIC_URL"),
            System.getenv("MYSQL_PUBLIC_URL"),
            environment.getProperty("MYSQL_URL"),
            System.getenv("MYSQL_URL")
        );
        if (mysqlUrl != null && mysqlUrl.contains("railway.internal")) {
            String publicUrl = firstNonBlank(
                environment.getProperty("MYSQL_PUBLIC_URL"),
                System.getenv("MYSQL_PUBLIC_URL")
            );
            if (publicUrl != null && !publicUrl.isBlank()) {
                mysqlUrl = publicUrl;
            } else {
                // Don't set spring.datasource.* from private URL; fallback config will throw clear error
                mysqlUrl = null;
            }
        }
        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            Map<String, Object> props = parseToSpringDatasource(mysqlUrl, environment);
            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(
                    new MapPropertySource("railwayMysql", props)
                );
            }
            return;
        }
        // Fallback: build from Railway's separate vars (MYSQLHOST, MYSQLUSER, ...)
        String host = envOrProp(environment, "MYSQLHOST");
        String port = envOrProp(environment, "MYSQLPORT");
        String user = envOrProp(environment, "MYSQLUSER");
        String pass = envOrProp(environment, "MYSQLPASSWORD");
        String database = envOrProp(environment, "MYSQLDATABASE");
        if (host != null && user != null && pass != null && database != null && !host.contains("railway.internal")) {
            String url = "jdbc:mysql://" + host + ":" + (port != null ? port : "3306") + "/" + database + "?useSSL=true";
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", url);
            props.put("spring.datasource.username", user);
            props.put("spring.datasource.password", pass);
            environment.getPropertySources().addFirst(new MapPropertySource("railwayMysql", props));
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static Map<String, Object> parseToSpringDatasource(String mysqlUrl, ConfigurableEnvironment environment) {
        Map<String, Object> props = new HashMap<>();
        if (mysqlUrl.startsWith("jdbc:mysql:")) {
            props.put("spring.datasource.url", mysqlUrl);
            String user = firstNonBlank(environment.getProperty("MYSQLUSER"), System.getenv("MYSQLUSER"));
            String pass = firstNonBlank(environment.getProperty("MYSQLPASSWORD"), System.getenv("MYSQLPASSWORD"));
            if (user != null) props.put("spring.datasource.username", user);
            if (pass != null) props.put("spring.datasource.password", pass);
        } else if (mysqlUrl.startsWith("mysql://")) {
            try {
                URI uri = URI.create(mysqlUrl.replace("mysql://", "http://"));
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
                throw new IllegalStateException("Invalid MYSQL URL: " + e.getMessage(), e);
            }
        }
        return props;
    }

    private static String envOrProp(ConfigurableEnvironment environment, String name) {
        String v = environment.getProperty(name);
        if (v != null && !v.isBlank()) return v;
        return System.getenv(name);
    }
}
