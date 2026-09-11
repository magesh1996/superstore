package com.superstore.app.config.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class BeanLoggingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(BeanLoggingInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String[] beanNames = context.getBeanFactory().getBeanDefinitionNames();
        logger.info("===== TOTAL REGISTERED BEANS : {} =====", beanNames.length);
        
        for (String name : beanNames) {
            logger.info("BEAN NAME : {} | TYPE : {}", name, context.getBeanFactory().getBeanDefinition(name).getBeanClassName());
        }
    }
}