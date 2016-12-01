package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface LicensePluginContext {

    File getAppBasedir();

    File getLicenseFilePath();

    String getLicenseFileName();

    File getMapperProperties();

    MavenProject getProject();

    MavenSession getSession();
    
    void info(String text);

}
