package com.cumulocity.maven3.plugin.thirdlicense.jar;

import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapper;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;

/**
 * Class is simple DTO object only to transfer data.
 */
public class Jar {

    private final String separator;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String copyright;
    private final String license;
    private final String fileName;
    private final String absolutePath;
    private final String relativePath;
    private final String usOrigin;

    private Jar(String separator, String groupId, String artifactId, String version, String copyright, String license, String fileName, String absolutePath, String relativePath, String usOrigin) {
        this.separator = separator;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.copyright = copyright;
        this.license = license;
        this.fileName = fileName;
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;
        this.usOrigin = usOrigin;
    }

    public static Jar of(Path jarPath, Path basedir, PropertyMapper propertyMapper) {
        return new Jar(
                Jars.toSeparator(jarPath)
                , Jars.toGroupId(jarPath, propertyMapper)
                , Jars.toArtifactId(jarPath, propertyMapper)
                , Jars.toVersion(jarPath, propertyMapper)
                , Jars.toCopyright(jarPath, propertyMapper)
                , Jars.toLicense(jarPath, propertyMapper)
                , Jars.toFileName(jarPath)
                , Jars.toAbsolutePath(jarPath)
                , Jars.toRelativePath(jarPath, basedir)
                , Jars.toUsOrigin(jarPath, propertyMapper)
        );
    }

    public String getFileName() {
        return fileName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getLicense() {
        return license;
    }
    
    public String getUsOrigin() {
        return usOrigin;
    }

    public boolean isCumulocityJar() {
        return isCumulocityInternalDependency()
                || isCumulocityAgentInternalDependency();
    }

    public boolean isCumulocityAgentInternalDependency() {
        return getGroupId() != null
                && (getGroupId().startsWith("c8y.agents")
                || getGroupId().startsWith("c8y-agents"));
    }

    private boolean isCumulocityInternalDependency() {
        return getGroupId() != null
                && getGroupId().startsWith("com.nsn.cumulocity")
                && !getGroupId().startsWith("com.nsn.cumulocity.dependencies.osgi");
    }

    public boolean isThirdPartyRepackedJar() {
        return getGroupId() != null && getGroupId().startsWith("com.nsn.cumulocity.dependencies.osgi");
    }

    public Jar stripCumulocityVersion() {
        final String strippedVersion = StringUtils.substringBefore(version, "-");
        return new Jar(
                separator,
                groupId,
                artifactId,
                strippedVersion,
                copyright,
                license,
                fileName.replace(version, strippedVersion),
                absolutePath,
                relativePath,
                usOrigin
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Jar jar = (Jar) o;
        return absolutePath.equals(jar.absolutePath);
    }

    @Override
    public int hashCode() {
        return absolutePath.hashCode();
    }

    @Override
    public String toString() {
        return "Jar{" + relativePath + separator + fileName + "}";
    }
}
