package com.cumulocity.maven3.plugin.thirdlicense.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.cumulocity.maven3.plugin.thirdlicense.license.JarTo3PartyInformation;
import com.cumulocity.maven3.plugin.thirdlicense.license.JarTo3PartyInformation.MissingJarProperty;

public class Validator {
    
    public static void validate(Log log, List<Jar> jars) {
        List<MissingJarProperty> missingProperties = new ArrayList<MissingJarProperty>();
        for (Jar jar : jars) {
            missingProperties.addAll(JarTo3PartyInformation.getMissingRequiredJarProperties(jar));
        }
        if (!missingProperties.isEmpty()) {
            String errorDetails = asErrorDetails(missingProperties);
            log.error("There are missing 3rd party informations: " + errorDetails);
            log.error("Please add missing entries to the mapper.properties file in the source of 3rd-license-maven-plugin or "
                    + " - as a temporary workaround - to the file in the current project (src/main/resources/license/mapper.properties).");
            throw new RuntimeException("There are missing 3rd party informations!");
        }

    }

    private static String asErrorDetails(List<MissingJarProperty> allMissingInformations) {
        String lineSeparator = System.getProperty("line.separator");
        StringBuilder result = new StringBuilder();
        for (MissingJarProperty missingInformation : allMissingInformations) {
            result.append(lineSeparator);
            result.append(missingInformation);
        }
        return result.toString();
    }

}
