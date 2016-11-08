package com.cumulocity.maven3.plugin.thirdlicense.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyMapper {

    private final Properties properties;

    public PropertyMapper(File definition) {
        properties = new Properties();
        if (definition.exists() && definition.isFile()) {
            try {
                properties.load(new FileInputStream(definition));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read from " + definition.getAbsolutePath());
            }
        }
    }

    public String mapGroupId(String jarName, String defaultValue) {
        return properties.getProperty(groupIdKey(jarName), defaultValue);
    }

    private String groupIdKey(String jarName) {
        return jarName + ".groupId";
    }

    public String mapArtifactId(String jarName, String defaultValue) {
        return properties.getProperty(artifactIdKey(jarName), defaultValue);
    }

    private String artifactIdKey(String jarName) {
        return jarName + ".artifactId";
    }

    public String mapVersion(String jarName, String defaultValue) {
        return properties.getProperty(versionKey(jarName), defaultValue);
    }

    private String versionKey(String jarName) {
        return jarName + ".version";
    }

    public String mapCopyright(String jarName, String defaultValue) {
        return properties.getProperty(copyrightKey(jarName), defaultValue);
    }

    private String copyrightKey(String jarName) {
        return jarName + ".copyright";
    }

    public String mapValueForCopyright(String copyright) {
        return properties.getProperty(copyrightValueKey(copyright), copyright);
    }

    private String copyrightValueKey(String value) {
        return (value == null ? null : getValue(value)) + ".value";
    }

    private String getValue(String value) {
        return value.replaceAll(" ", "_").replaceAll(":", "_");
    }

    public String mapLicense(String jarName, String defaultValue) {
        return properties.getProperty(licenseKey(jarName), defaultValue);
    }

    public String licenseKey(String jarName) {
        return jarName + ".license";
    }

    public String mapValueForLicense(String license) {
        return properties.getProperty(licenseValueKey(license), license);
    }

    private String licenseValueKey(String value) {
        return (value == null ? null : getValue(value)) + ".value";
    }
}
