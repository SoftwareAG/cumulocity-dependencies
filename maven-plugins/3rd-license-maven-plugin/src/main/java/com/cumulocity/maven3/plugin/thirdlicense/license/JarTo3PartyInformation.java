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
    
    private static final String LINE_FORMAT = "%s, %s:%s:%s, %s, %s, %s";
    
    @Nullable
    @Override
    public String apply(Jar jar) {
        return String.format(LINE_FORMAT, (Object[]) getPrintableProperties(jar));
    }
    
    
    public static List<MissingJarProperty> getMissingJarProperties(Jar jar) {
        List<MissingJarProperty> missings = new ArrayList<MissingJarProperty>();
        for (JarProperty property : getPrintableProperties(jar)) {
            checkProperty(missings, jar.getFileName(), property);            
        }
        return missings;
    }
    
    private static void checkProperty(List<MissingJarProperty> missings, String fileName, JarProperty property) {
        if (Jars.UNKNOWN_VALUE.equals(property.toString())) {
            missings.add(new MissingJarProperty(fileName, property.getLabel()));
        }
    }

    
    private static  JarProperty[] getPrintableProperties(Jar jar) {
        return  new JarProperty[] {
            // @formatter:off
            new JarProperty(jar.getFileName(), "fileName"),
            new JarProperty(jar.getGroupId(), "groupId"),
            new JarProperty(jar.getArtifactId(), "artifactId"),
            new JarProperty(jar.getVersion(), "version"),
            new JarProperty(jar.getCopyright(), "copyright"),
            new JarProperty(jar.getLicense(), "license"),
            new JarProperty(jar.getUsOrigin(), "usOrigin")
            // @formatter:on
        };
    }
    
    static class JarProperty {
        
        private final String value;
        private final String label;
        
        public JarProperty(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return value == null ? "" : value.trim();
        }
    }
    
    public static class MissingJarProperty {
        
        private final String fileName;
        private final String information;
        
        public MissingJarProperty(String fileName, String information) {
            this.fileName = fileName;
            this.information = information;
        }
        
        @Override
        public String toString() {
            return String.format("artifact: %s, property: %s", fileName, information);
        }
    }

}
