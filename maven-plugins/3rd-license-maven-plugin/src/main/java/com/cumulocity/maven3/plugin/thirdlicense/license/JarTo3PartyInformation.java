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
    
    private static final String LINE_FORMAT = "%s, %s:%s:%s, %s, %s, %s, %s";
    private static final String[] HEADERS = new String[]{"fileName", "groupId", "artifactId",
            "version", "copyright", "license", "usOrigin", "cryptography"};
    
    @Nullable
    @Override
    public String apply(Jar jar) {
        return String.format(LINE_FORMAT, (Object[]) getPrintableProperties(jar));
    }

    public static String getHeadersLine() {
        return String.format(LINE_FORMAT, HEADERS);
    }

    public static List<MissingJarProperty> getMissingRequiredJarProperties(Jar jar) {
        List<MissingJarProperty> missings = new ArrayList<MissingJarProperty>();
        checkProperty(missings, jar.getFileName(), new JarProperty(jar.getFileName(), HEADERS[0]));
        checkProperty(missings, jar.getFileName(), new JarProperty(jar.getCopyright(), HEADERS[4]));
        checkProperty(missings, jar.getFileName(), new JarProperty(jar.getLicense(), HEADERS[5]));
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
            new JarProperty(jar.getFileName(), HEADERS[0]),
            new JarProperty(jar.getGroupId(), HEADERS[1]),
            new JarProperty(jar.getArtifactId(), HEADERS[2]),
            new JarProperty(jar.getVersion(), HEADERS[3]),
            new JarProperty(jar.getCopyright(), HEADERS[4]),
            new JarProperty(jar.getLicense(), HEADERS[5]),
            new JarProperty(jar.getUsOrigin(), HEADERS[6]),
            new JarProperty(jar.getCryptography(), HEADERS[7])
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
            return value == null ? "" : escapeCommas(value.trim());
        }

        private String escapeCommas(String value) {
            if (value.contains(",")) {
                value = value.replace("\"", "");
                return "\"" + value + "\"";
            }
            return value;
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
