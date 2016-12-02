package com.cumulocity.maven3.plugin.thirdlicense;

import static com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext.PROPERTY_KEY_PREFIX;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;
import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContextImpl;
import com.cumulocity.maven3.plugin.thirdlicense.diff.DiffService;
import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.cumulocity.maven3.plugin.thirdlicense.jar.Jars;
import com.cumulocity.maven3.plugin.thirdlicense.license.JarTo3PartyInformation;
import com.cumulocity.maven3.plugin.thirdlicense.license.Licenses;
import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapper;
import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapperFactory;

/**
 * This is main class for maven plugin, from this file maven start work with
 * this feature
 *
 * @goal generate
 * @phase prepare-package
 */
@Mojo(name = "3rd-license-generate", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class Generate3rdLicenseMojo extends AbstractMojo {

    @Parameter(alias = "app.basedir", defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File appBasedir;

    @Parameter(alias = "third.party.license.file.path", defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File thirdPartyLicenseFilePath;

    @Parameter(alias = "third.party.license.file.name", defaultValue = "THIRD-PARTY-LICENSES")
    private String thirdPartyLicenseFileName;

    @Parameter(alias = "mapper.properties", defaultValue = "${basedir}/src/main/resources/license/mapper.properties")
    private File mapperProperties;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private LicensePluginContext pluginContext;

    @Component
    private DiffService diffService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initContext();
        if (!"TRUE".equalsIgnoreCase(pluginContext.getProperty("enabled"))) {
            return;
        }
        getLog().info("Generate 3rd part libraries");
        

        checkNotNull(appBasedir, "Cannot work on undefined: app.basedir");
        getLog().info("Reading libraries from " + appBasedir.getAbsolutePath());

        final PropertyMapper mapper = PropertyMapperFactory.create(getLog(), mapperProperties);

        final List<Jar> jars = new ArrayList<>();

        Jars.walkJarTree(appBasedir, new Jars.JarFileVisitor() {
            @Override
            public void visitJar(Path jarPath) {
                Jar jar = Jar.of(jarPath, appBasedir.getAbsoluteFile().toPath(), mapper);
                if (!jar.isCumulocityJar()) {
                    jars.add(jar);
                }
            }
        });

        getLog().info("Save 3rd-party-file " + thirdPartyFile());
        thirdPartyLicenseFilePath.mkdirs();
        Licenses.save(thirdPartyFile(), jars, new JarTo3PartyInformation());
        // Validator.validate(getLog(), jars);
        diffService.execute();
    }

    private Path thirdPartyFile() {
        return Paths.get(thirdPartyLicenseFilePath.getAbsoluteFile().getAbsolutePath(), thirdPartyLicenseFileName);
    }

    private void checkNotNull(Object object, String message) throws MojoFailureException {
        if (object == null) {
            throw new MojoFailureException(message);
        }
    }

    private void initContext() {
        LicensePluginContextImpl pluginContextImpl = (LicensePluginContextImpl) pluginContext;
        pluginContextImpl.setAppBasedir(appBasedir);
        pluginContextImpl.setLicenseFilePath(thirdPartyLicenseFilePath);
        pluginContextImpl.setLicenseFileName(thirdPartyLicenseFileName);
        pluginContextImpl.setMapperProperties(mapperProperties);
        pluginContextImpl.setProject(project);
        pluginContextImpl.setSession(mavenSession);
        pluginContextImpl.setLog(getLog());
        pluginContextImpl.setProperties(initProperties());
        getLog().info("Plaugin setup: " + pluginContextImpl);
    }

    /**
     * Read all properties from settings.xml from active profiles and with
     * prefix "3rdLicense."
     */
    @SuppressWarnings("unchecked")
    private Properties initProperties() {
        Properties properties = new Properties();
        List<Profile> activeProfiles = (List<Profile>) project.getActiveProfiles();
        for (Profile profile : activeProfiles) {
            Properties profileProperties = profile.getProperties();
            for (String key : profileProperties.stringPropertyNames()) {
                if (key.startsWith(PROPERTY_KEY_PREFIX)) {
                    String pluginPropKey = key.substring(PROPERTY_KEY_PREFIX.length());
                    String pluginPropValue = profileProperties.getProperty(key);
                    properties.setProperty(pluginPropKey, pluginPropValue);
                }
            }
        }
        return properties;
    }
}
