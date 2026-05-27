package com.outbound.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton Pattern — ConfigManager
 *
 * Why Singleton here:
 *   The properties file is read from disk once and cached for the lifetime
 *   of the test run. Creating a new ConfigManager per test class would
 *   re-read the file repeatedly and risk inconsistency if the file changes
 *   mid-run (unlikely but possible in CI with mounted volumes).
 *
 * Thread safety:
 *   Initialised via the "initialization-on-demand holder" idiom — the JVM
 *   guarantees the inner static class is loaded lazily and exactly once,
 *   with no synchronisation overhead on reads.
 *
 * Override precedence (highest to lowest):
 *   1. JVM system property  (-Dbase.url=https://...)
 *   2. config.properties file
 *   3. Default value passed to get()
 */
public class ConfigManager {

    private final Properties props = new Properties();

    private ConfigManager() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    // Initialization-on-demand holder — lazy, thread-safe, no synchronisation needed
    private static final class Holder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the value for the given key.
     * System properties take precedence over the properties file,
     * which takes precedence over the supplied default.
     */
    public String get(String key, String defaultValue) {
        // System property wins — allows CI to override without changing the file
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) return sysProp;
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
