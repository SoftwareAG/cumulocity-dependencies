package com.cumulocity.maven3.plugin.thirdlicense.license;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.cumulocity.maven3.plugin.thirdlicense.jar.Jars;
import com.google.common.base.Function;

/**
 * Class convert object {@see Jar} to String representation.
 */
public class JarTo3PartyInformation implements Function<Jar, String> {

    private static final String LINE_FORMAT = "%s, %s:%s:%s, %s, %s";

    @Nullable
    @Override
    public String apply(Jar jar) {
        return String.format(LINE_FORMAT, jar.getFileName(), jar.getGroupId(), jar.getArtifactId(), jar.getVersion(), jar.getCopyright(), jar.getLicense());
    }
    
    
    public static List<MissingInformation> getMissingInformations(Jar jar) {
        List<MissingInformation> missings = new ArrayList<MissingInformation>();
        checkProperty(missings, jar.getFileName(), jar.getGroupId(), "groupId");
        checkProperty(missings, jar.getFileName(), jar.getArtifactId(), "artifactId");
        checkProperty(missings, jar.getFileName(), jar.getVersion(), "version");
        checkProperty(missings, jar.getFileName(), jar.getCopyright(), "copyright");
        checkProperty(missings, jar.getFileName(), jar.getLicense(), "license");
        return missings;
    }
    
    private static void checkProperty(List<MissingInformation> missings, String fileName, String value, String propertyName) {
        if (Jars.UNKNOWN_VALUE.equals(value)) {
            missings.add(new MissingInformation(fileName, propertyName));
        }
    }
    
    public static class MissingInformation {
        
        private final String fileName;
        private final String information;
        
        public MissingInformation(String fileName, String information) {
            this.fileName = fileName;
            this.information = information;
        }

        @Override
        public String toString() {
            return String.format("fileName: %s, information: %s", fileName, information);
        }
        
        
    }

}
