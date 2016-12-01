package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface LicensePluginContext {

    File getAppBasedir();

    File getLicenseFilePath();

    String getLicenseFileName();

    File getMapperProperties();

    MavenProject getProject();

    MavenSession getSession();
    
    Properties getSettingsProperties();
    
    void info(String text);
    
    void warn(String text);

}
