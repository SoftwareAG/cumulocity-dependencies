package com.cumulocity.maven3.plugin.thirdlicense.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyMapperFactory {

    public static PropertyMapper create(File... definitions) {
        Properties[] props = new Properties[definitions.length];
        for(int i = 0; i < definitions.length; i++) {
            props[i] = load(definitions[i]);
        }
        return new PropertyMapper(props);
    }

    private static Properties load(File definition) {
        Properties properties = new Properties();
        if (definition.exists() && definition.isFile()) {
            try {
                properties.load(new FileInputStream(definition));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read from " + definition.getAbsolutePath());
            }
        }
        return properties;
    }
}
