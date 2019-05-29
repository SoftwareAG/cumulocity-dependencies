package com.cumulocity.maven3.plugin.thirdlicense.jar;

import com.cumulocity.maven3.plugin.thirdlicense.artifact.Artifacts;
import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;

import static com.cumulocity.maven3.plugin.thirdlicense.artifact.Artifacts.isThirdPartyRepackedArtifact;
import static com.cumulocity.maven3.plugin.thirdlicense.jar.Jars.DISTRIBUTED_USAGE_TYPE;
import static com.cumulocity.maven3.plugin.thirdlicense.jar.Jars.INTERNAL_USAGE_TYPE;

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
    private final String palamidaId;
    private final String zCode;
    private final String usageType;
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
                , Jars.toPalamidaId(jarPath, propertyMapper)
                , Jars.toZCode(jarPath, propertyMapper)
                , resolveUsageType(groupId)
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
        final String strippedVersion = StringUtils.substringBefore(version, "-");
        final String strippedFileName = fileName.replace(version, strippedVersion);
        return new Jar(
                separator,
                groupId,
                artifactId,
                strippedVersion,
                Jars.toPalamidaId(strippedFileName, propertyMapper),
                Jars.toZCode(strippedFileName, propertyMapper),
                usageType,
                copyright,
                license,
                strippedFileName,
                absolutePath,
                relativePath,
                usOrigin,
                cryptography
        );
    }

    private static String resolveUsageType (String groupId) {
        return Artifacts.isThirdPartyRepackedArtifact(groupId) ? INTERNAL_USAGE_TYPE : DISTRIBUTED_USAGE_TYPE;
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
