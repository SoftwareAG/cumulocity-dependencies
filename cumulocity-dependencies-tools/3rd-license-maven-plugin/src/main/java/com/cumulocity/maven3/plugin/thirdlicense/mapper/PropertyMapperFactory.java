package com.cumulocity.maven3.plugin.thirdlicense.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;

public class PropertyMapperFactory {

    private static final String DEFAULT_MAPPER_PROPERTIES = "/META-INF/mapper.properties";

    public static PropertyMapper create(Log log, File mapperProperties) {
        Properties defaultMapperProperties = loadDefaultMapperProperties();
        if (mapperProperties.exists() && mapperProperties.isFile()) {
            log.info("Reading mapping from: " + mapperProperties.getAbsolutePath());
            return new PropertyMapper(defaultMapperProperties, load(mapperProperties));
        } else {
            log.info("Use default mapping properties from the plugin source: " + DEFAULT_MAPPER_PROPERTIES);
            return new PropertyMapper(defaultMapperProperties);
        }
    }

    private static Properties loadDefaultMapperProperties() {
        Properties defaultMapperProperties = new Properties();
        try {
            defaultMapperProperties.load(PropertyMapperFactory.class.getResourceAsStream(DEFAULT_MAPPER_PROPERTIES));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read from default properties from " + DEFAULT_MAPPER_PROPERTIES);
        }
        return defaultMapperProperties;
    }

    private static Properties load(File definition) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(definition));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read from " + definition.getAbsolutePath());
        }
        return properties;
    }
}
