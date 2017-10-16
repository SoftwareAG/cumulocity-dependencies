package com.cumulocity.maven3.plugin.thirdlicense.license;

import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class only save Collection of {@see Jar} to file.
 * Object {@see Jar} is converted to string representation by function {@see Function}
 *
 * @see JarTo3PartyInformation
 */
public class Licenses {

    public static void save(Log log, Path path, Collection<Jar> jars, Function<Jar, String> toLine) throws MojoFailureException {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Collection<String> lines = Collections2.transform(jars, toLine);
            lines = Ordering.natural().sortedCopy(lines);
            Files.write(path, getLinesWithHeader(lines), Charsets.UTF_8);
            log.info("Write 3rd party license file to " + path);
        } catch (IOException ioe) {
            throw new MojoFailureException("Cannot create " + path + " file", ioe);
        }
    }

    private static List<String> getLinesWithHeader(Collection<String> lines) {
        List<String> result = new ArrayList<>();
        result.add(JarTo3PartyInformation.getHeadersLine());
        result.addAll(lines);
        return result;
    }

}
