package io.github.allbegray.pebbleforge.demoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Pebble Forge demo web application.
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * Creates the application configuration.
     */
    public DemoApplication() {
    }

    /**
     * Starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
