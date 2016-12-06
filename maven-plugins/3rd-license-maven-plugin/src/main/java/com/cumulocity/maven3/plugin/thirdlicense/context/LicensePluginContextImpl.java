package com.cumulocity.maven3.plugin.thirdlicense.context;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
@Component( role = LicensePluginContext.class )
public class LicensePluginContextImpl implements LicensePluginContext {
    
    private File appBasedir;
    
    private File licenseFilePath;

    private String licenseFileName;
    
    private String licenseFileTargetType;

    private File mapperProperties;
    
    private MavenProject project;

    private MavenSession session;
    
    private Log log;
    
    private Properties properties;

    public void info(String text) {
        log.info(text);
    }
    
    @Override
    public void warn(String text) {
        log.warn(text);
    }
    
    @Override
    public boolean hasProperty(String key) {
        return StringUtils.isNotBlank(getProperty(key));
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        if (hasProperty(key)) {
            return Boolean.valueOf(getProperty(key));
        }
        return defaultValue;
    }
    
    
    

}
