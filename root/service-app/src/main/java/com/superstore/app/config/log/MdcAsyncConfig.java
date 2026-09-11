package com.superstore.app.config.log;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class MdcAsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("appASYNC-");
        
        // link the decorator.
        executor.setTaskDecorator(new MdcTaskDecorator()); 
        
        executor.initialize();
        return executor;
    }

    // this decorator automatically intercepts context handoffs between threads.
    public static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // capture the entire MDC map from the main thread right before execution splits.
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        // bind it to the background ASYNC thread.
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    // clean it up when the ASYNC thread finishes.
                    // SAFE to clear here because this thread is isolated to the task.
                    MDC.clear();
                }
            };
        }
    }
}