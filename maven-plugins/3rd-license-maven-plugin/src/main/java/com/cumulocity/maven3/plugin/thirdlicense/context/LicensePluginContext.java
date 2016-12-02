package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface LicensePluginContext {
    
    public static String PROPERTY_KEY_PREFIX = "third.party.license.";

    File getAppBasedir();

    File getLicenseFilePath();

    String getLicenseFileName();

    File getMapperProperties();

    MavenProject getProject();

    MavenSession getSession();
    
    void info(String text);
    
    void warn(String text);
    
    String getProperty(String key);
    
    boolean hasProperty(String key);
    
    Properties getProperties();

}
