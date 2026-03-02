package com.hospital.cdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Application Entrypoint
 * 
 * Configures the Spring Context and implements initialization handlers
 * required for WAR packaging and external Application Server deployment.
 */
@SpringBootApplication
public class BuscadorApplication extends SpringBootServletInitializer {

    /**
     * Binds the application to external Servlet containers (e.g. Apache Tomcat).
     * This is mandatory since the application is packaged as a standard WAR
     * for production environments, bypassing the embedded Tomcat engine.
     * 
     * @param application Application Builder Context
     * @return Configured Spring Application Builder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BuscadorApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(BuscadorApplication.class, args);
    }
}
