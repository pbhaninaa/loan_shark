package com.loanshark.api.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * Fails fast with a clear message when running locally (sit profile) without a DB password set,
 * before the DataSource tries to connect.
 */
@Component
public class LocalDatasourcePasswordCheck implements BeanFactoryPostProcessor, PriorityOrdered, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Environment env = applicationContext.getEnvironment();
        String activeProfile = env.getProperty("spring.profiles.active", "");
        if (activeProfile == null || !activeProfile.contains("sit")) {
            return;
        }
        String url = env.getProperty("spring.datasource.url", "");
        String password = env.getProperty("spring.datasource.password", "");
        if (url == null || !url.contains("localhost")) {
            return;
        }
        if (password != null && !password.isBlank()) {
            return;
        }
        throw new IllegalStateException(
            "Local MySQL password is not set. Copy application-local.example.properties to application-local.properties "
            + "and set spring.datasource.password=YourMySQLRootPassword (see services/api/src/main/resources/application-local.example.properties)."
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
