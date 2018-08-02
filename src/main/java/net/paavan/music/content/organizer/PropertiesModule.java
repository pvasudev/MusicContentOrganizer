package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class PropertiesModule extends AbstractModule {
    private static final String PROPERTIES_FILE = "config.properties";

    @Override
    protected void configure() {
        Names.bindProperties(binder(), getProperties());
    }

    // --------------
    // Helper Methods

    private Properties getProperties() {
        Properties allProperties = readPropertiesFile();
        Properties hostSpecificProperties = new Properties();

        for (Object key : allProperties.keySet()) {
            String stringKey = (String) key;
            if (stringKey.startsWith("*.")) {
                hostSpecificProperties.put(stringKey.substring(2), allProperties.get(key));
            } else if (stringKey.contains(getHostName())) {
                hostSpecificProperties.put(stringKey.substring(getHostName().length() + 1), allProperties.get(key));
            }
        }

        return hostSpecificProperties;
    }

    private Properties readPropertiesFile() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = classLoader.getResourceAsStream(PROPERTIES_FILE)) {
            properties.load(resourceStream);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return properties;
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
