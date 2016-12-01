package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import lombok.Setter;


@Setter
@Component( role = LicensePluginContext.class )
public class LicensePluginContextImpl implements LicensePluginContext {
    
    private File appBasedir;
    
    private File licenseFilePath;

    private String licenseFileName;

    private File mapperProperties;
    
    private MavenProject project;

    private MavenSession session;
    
    private Log log;

    @Override
    public File getAppBasedir() {
        return appBasedir;
    }

    @Override
    public File getLicenseFilePath() {
        return licenseFilePath;
    }

    @Override
    public String getLicenseFileName() {
        return licenseFileName;
    }

    @Override
    public File getMapperProperties() {
        return mapperProperties;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public MavenSession getSession() {
        return session;
    }

    @Override
    public void info(String text) {
        log.info(text);
    }

}
