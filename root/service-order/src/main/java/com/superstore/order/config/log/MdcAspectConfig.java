package com.superstore.order.config.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MdcAspectConfig {

    // this pointcut intercepts all public methods inside classes annotated with @Service.
    @Around("within(@org.springframework.stereotype.Service *) && execution(public * *(..))")
    public Object manageMdcContext(ProceedingJoinPoint joinPoint) throws Throwable {
        
        Class<?> targetClass = joinPoint.getTarget().getClass();
        String fullPackageName = targetClass.getPackageName();
        
        // dynamically extracts "SERVICE_NAME" from "com.superstore.SERVICE_NAME".
        String SERVICE_NAME = "general";
        if (fullPackageName.contains("com.superstore.")) {
            String[] parts = fullPackageName.split("\\.");
            if (parts.length > 2) {
                SERVICE_NAME = parts[2];
            }
        }

        String serviceName = "service-" + SERVICE_NAME;
        // extract and format the ClassName.
        // String fullLoggerName = targetClass.getName();
        // String loggerName = fullLoggerName.replaceAll("\\b\\w+\\.(?=\\w+\\.)", "");
        // FIX : use getSimpleName() instead of the replaceAll regex.
        String loggerName = targetClass.getSimpleName();

        // push it into MDC before the service method runs.
        MDC.put("serviceName", serviceName);
        MDC.put("loggerName", loggerName);

        try {
            return joinPoint.proceed(); // executes our actual service method.
        } finally {
            // automatically clears the MDC context when the service method finishes.
            //MDC.clear();
            // ONLY remove what we added so we don't corrupt upstream filters.
            MDC.remove("serviceName");
            MDC.remove("loggerName");
        }
    }
}