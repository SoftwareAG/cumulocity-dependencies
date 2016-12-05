package com.cumulocity.maven3.plugin.thirdlicense.jar;

import com.cumulocity.maven3.plugin.thirdlicense.mapper.PropertyMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.apache.maven.plugin.MojoFailureException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class is responsible for obtain information from jar file.
 */
public class Jars {

    public static final String UNKNOWN_VALUE = "UNKNOWN";

    public static String toGroupId(Path jarPath, PropertyMapper propertyMapper) {
        String groupId = propertyMapper.mapGroupId(toFileName(jarPath), UNKNOWN_VALUE);

        if (isUnknown(groupId)) {
            groupId = getPropertyFromPomProperties(jarPath, "groupId");
        }

        if (isUnknown(groupId)) {
            String part1 = getPropertyFromManifest(jarPath, "Bundle-SymbolicName");
            groupId = part1.replaceFirst(toArtifactId(jarPath, propertyMapper), "");
            if ("".equals(groupId)) {
                groupId = UNKNOWN_VALUE;
            }
            if (groupId.lastIndexOf(".") == groupId.length() - 1) {
                groupId = groupId.substring(0, groupId.length() - 1);
            }
        }

        return groupId;
    }

    private static boolean isUnknown(String value) {
        return UNKNOWN_VALUE.equals(value);
    }

    public static String toArtifactId(Path jarPath, PropertyMapper propertyMapper) {
        String artifactId = propertyMapper.mapArtifactId(toFileName(jarPath), UNKNOWN_VALUE);

        if (isUnknown(artifactId)) {
            artifactId = getPropertyFromPomProperties(jarPath, "artifactId");
        }

        if (isUnknown(artifactId)) {
            String fileName = toFileName(jarPath);
            artifactId = fileName.replace("-" + toVersion(jarPath, propertyMapper) + ".jar", "");
        }

        return artifactId;
    }

    public static String toVersion(Path jarPath, PropertyMapper propertyMapper) {
        String version = propertyMapper.mapVersion(toFileName(jarPath), UNKNOWN_VALUE);

        if (isUnknown(version)) {
            version = getPropertyFromPomProperties(jarPath, "version");
        }

        if (isUnknown(version)) {
            String fileName = toFileName(jarPath);
            Matcher matcher = Pattern.compile("[\\d,\\.,\\-,v]+[A-Z,_,\\d]+\\.jar$").matcher(fileName);
            if (matcher.find()) {
                version = fileName.substring(matcher.start() + 1).replace(".jar", "");
            } else {
                version = UNKNOWN_VALUE;
            }
        }

        if (isUnknown(version)) {
            version = getPropertyFromManifest(jarPath, "Bundle-Version");
        }

        return version;
    }

    public static String toCopyright(Path jarPath, PropertyMapper propertyMapper) {
        String copyright = propertyMapper.mapCopyright(toFileName(jarPath), UNKNOWN_VALUE);

        if (isUnknown(copyright)) {
            copyright = getPropertyFromManifest(jarPath, "Bundle-Vendor");
        }

        return propertyMapper.mapValueForCopyright(copyright);
    }

    public static String toLicense(Path jarPath, PropertyMapper propertyMapper) {
        String license = propertyMapper.mapLicense(toFileName(jarPath), UNKNOWN_VALUE);

        if (isUnknown(license)) {
            license = readLines(jarPath, "LICENSE.txt", 2, 3);
        }

        if (isUnknown(license)) {
            license = getPropertyFromManifest(jarPath, "Bundle-License");
        }

        if (isUnknown(license)) {
            license = readLines(jarPath, "license.txt", 1, 2);
        }

        return propertyMapper.mapValueForLicense(license);
    }
    
    public static String toUsOrigin(Path jarPath, PropertyMapper propertyMapper) {
        return propertyMapper.mapUsOrigin(toFileName(jarPath), UNKNOWN_VALUE);
    }


    private static String getPropertyFromPomProperties(Path jarPath, String propertyKey) {
        try {
            JarFile jarFile = getJarFile(jarPath);
            JarEntry jarEntry = getEntry(jarFile, "pom.properties");
            String propertyValue = getPropertyFromPomProperties(jarFile, jarEntry, propertyKey);
            return propertyValue;
        } catch (Exception e) {
            return UNKNOWN_VALUE;
        }
    }

    private static String readLines(Path jarPath, String file, Integer... lines) {
        try {
            JarFile jarFile = getJarFile(jarPath);
            JarEntry jarEntry = getEntry(jarFile, file);
            if (jarEntry == null) {
                return UNKNOWN_VALUE;
            }
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            List<String> text = CharStreams.readLines(new InputStreamReader(inputStream));

            final AtomicInteger lineNumber = new AtomicInteger(1);
            final List<Integer> linesAsList = Lists.newArrayList(lines);
            Collection<String> filteredText = Collections2.filter(text, new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String s) {
                    return linesAsList.contains(lineNumber.getAndIncrement());
                }
            });
            return Joiner.on(" ").join(Collections2.transform(filteredText, new Function<String, String>() {
                @Nullable
                @Override
                public String apply(@Nullable String s) {
                    return (s == null) ? "" : s.trim();
                }
            }));
        } catch (Exception e) {
            return UNKNOWN_VALUE;
        }
    }

    private static String getPropertyFromManifest(Path jarPath, String propertyKey) {
        try {
            JarFile jarFile = getJarFile(jarPath);
            Manifest manifest = jarFile.getManifest();
            String propertyValue = manifest.getMainAttributes().getValue(propertyKey);
            return (propertyValue == null) ? UNKNOWN_VALUE : propertyValue;
        } catch (Exception e) {
            return UNKNOWN_VALUE;
        }
    }

    private static JarFile getJarFile(Path jarPath) throws IOException {
        return new JarFile(jarPath.toFile());
    }

    private static JarEntry getEntry(JarFile jarFile, String entry) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().contains(entry)) {
                return jarEntry;
            }
        }
        return null;
    }

    private static String getPropertyFromPomProperties(JarFile jarFile, JarEntry jarEntry, String propertyKey) {
        try {
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            Properties props = new Properties();
            props.load(inputStream);
            return props.getProperty(propertyKey, UNKNOWN_VALUE);
        } catch (Exception e) {
            return UNKNOWN_VALUE;
        }
    }

    public static String toSeparator(Path path) {
        return path.getFileSystem().getSeparator();
    }

    public static String toFileName(Path path) {
        return path.getFileName().toString();
    }

    public static String toAbsolutePath(Path path) {
        return path.getParent().toString();
    }

    public static String toRelativePath(Path path, Path basedir) {
        return toAbsolutePath(path).replace(toAbsolutePath(basedir), "");
    }

    public static void walkJarTree(File basedir, final JarFileVisitor jarFileVisitor) throws MojoFailureException {
        try {
            Files.walkFileTree(basedir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    jarFileVisitor.visitFile(file, attrs);
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new MojoFailureException("Cannot read libraries", e);
        }
    }

    public static abstract class JarFileVisitor extends SimpleFileVisitor<Path> {
        public abstract void visitJar(Path jarPath);

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().endsWith(".jar")) {
                visitJar(file);
            }
            return super.visitFile(file, attrs);
        }
    }

}
