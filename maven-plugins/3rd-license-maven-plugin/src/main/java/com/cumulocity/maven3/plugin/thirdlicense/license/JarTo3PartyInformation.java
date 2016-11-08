package com.cumulocity.maven3.plugin.thirdlicense.license;

import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * Class convert object {@see Jar} to String representation.
 */
public class JarTo3PartyInformation implements Function<Jar, String> {

    @Nullable
    @Override
    public String apply(@Nullable Jar jar) {
        return jar.getFileName() + ", " +
                jar.getGroupId() + ":" + jar.getArtifactId() + ":" + jar.getVersion() + ", " +
                jar.getCopyright() + ", " +
                jar.getLicense();
    }

}
