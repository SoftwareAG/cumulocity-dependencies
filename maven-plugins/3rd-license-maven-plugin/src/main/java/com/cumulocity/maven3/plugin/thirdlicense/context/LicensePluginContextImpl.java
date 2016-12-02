package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import lombok.Setter;
import lombok.ToString;


@Setter
@ToString
@Component( role = LicensePluginContext.class )
public class LicensePluginContextImpl implements LicensePluginContext {
    
    private File appBasedir;
    
    private File licenseFilePath;

    private String licenseFileName;

    private File mapperProperties;
    
    private MavenProject project;

    private MavenSession session;
    
    private Log log;
    
    private Properties properties;

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
    
    @Override
    public void warn(String text) {
        log.warn(text);
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean hasProperty(String key) {
        return StringUtils.isNotBlank(getProperty(key));
    }

    

}
