package com.cumulocity.maven3.plugin.thirdlicense.jar;

import com.cumulocity.maven3.plugin.thirdlicense.artifact.Artifacts;
import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

import static com.cumulocity.maven3.plugin.thirdlicense.artifact.Artifacts.isThirdPartyRepackedArtifact;

/**
 * Class is simple DTO object only to transfer data.
 */
@Getter
@AllArgsConstructor
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
    private final String cryptography;


    public static Jar of(Path jarPath, Path basedir, PropertyMapper propertyMapper) {
        String groupId = Jars.toGroupId(jarPath, propertyMapper);
        return new Jar(
                Jars.toSeparator(jarPath)
                , groupId
                , Jars.toArtifactId(jarPath, propertyMapper)
                , Jars.toVersion(jarPath, propertyMapper)
                , Jars.toCopyright(jarPath, propertyMapper)
                , Jars.toLicense(jarPath, propertyMapper)
                , Jars.toFileName(jarPath)
                , Jars.toAbsolutePath(jarPath)
                , Jars.toRelativePath(jarPath, basedir)
                , Jars.toUsOrigin(jarPath, propertyMapper)
                , Jars.toCryptography(jarPath, propertyMapper)
        );
    }

    public boolean isCumulocityJar() {
        return Artifacts.isCumulocityArtifact(getGroupId());
    }

    public boolean isThirdPartyRepackedJar() {
        return isThirdPartyRepackedArtifact(getGroupId());
    }

    public Jar stripCumulocityVersion(PropertyMapper propertyMapper) {
        final String strippedVersion = Jars.stripCumulocityVersion(version);
        final String strippedFileName = fileName.replace(version, strippedVersion);
        return new Jar(
                separator,
                groupId,
                artifactId,
                strippedVersion,
                copyright,
                license,
                strippedFileName,
                absolutePath,
                relativePath,
                usOrigin,
                cryptography
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
