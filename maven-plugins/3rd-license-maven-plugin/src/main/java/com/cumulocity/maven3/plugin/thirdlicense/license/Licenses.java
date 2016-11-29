package com.cumulocity.maven3.plugin.thirdlicense.license;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.plugin.MojoFailureException;

import com.cumulocity.maven3.plugin.thirdlicense.jar.Jar;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;

/**
 * Class only save Collection of {@see Jar} to file.
 * Object {@see Jar} is converted to string representation by function {@see Function}
 *
 * @see JarTo3PartyInformation
 */
public class Licenses {

    public static void save(Path path, Collection<Jar> jars, Function<Jar, String> toLine) throws MojoFailureException {
        try {
            Files.createFile(path);
            Collection<String> lines = Collections2.transform(jars, toLine);
            lines = Ordering.natural().sortedCopy(lines);
            Files.write(path, lines, Charsets.UTF_8);
        } catch (IOException ioe) {
            throw new MojoFailureException("Cannot create " + path + " file", ioe);
        }
    }

}
