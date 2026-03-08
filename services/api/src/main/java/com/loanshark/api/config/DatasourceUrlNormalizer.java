package com.loanshark.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * If Railway (or the platform) injects a raw mysql:// URL into SPRING_DATASOURCE_URL,
 * convert it to jdbc:mysql:// so the MySQL JDBC driver accepts it. Does not replace
 * custom DB config — only normalizes the URL format when present.
 */
public class DatasourceUrlNormalizer implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        if (url == null) {
            url = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (url == null || !url.startsWith("mysql://")) {
            return;
        }
        try {
            URI uri = URI.create(url.replace("mysql://", "http://"));
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : "railway";
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=true";
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);
            if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
                int colon = uri.getUserInfo().indexOf(':');
                if (colon > 0) {
                    props.put("spring.datasource.username", uri.getUserInfo().substring(0, colon));
                    props.put("spring.datasource.password", uri.getUserInfo().substring(colon + 1));
                } else {
                    props.put("spring.datasource.username", uri.getUserInfo());
                }
            }
            environment.getPropertySources().addFirst(new MapPropertySource("datasourceUrlNormalizer", props));
        } catch (Exception ignored) {
            // Leave URL unchanged if parsing fails
        }
    }
}
