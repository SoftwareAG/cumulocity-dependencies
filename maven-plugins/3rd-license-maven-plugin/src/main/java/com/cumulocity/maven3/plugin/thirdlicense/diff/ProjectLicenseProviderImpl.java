package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

@Component(role = ProjectLicenseProvider.class)
public class ProjectLicenseProviderImpl implements ProjectLicenseProvider {
    
    @Requirement
    private LicensePluginContext ctx;
    
    @Requirement
    private BuildPluginManager pluginManager;

    public File get(String projectVersion)  {
        Path outputDirectory = Paths.get(ctx.getProject().getBuild().getDirectory(), "previousReleased");
        ctx.info("Download previous released version to directory " + outputDirectory);
        try {
            // @formatter:off
            executeMojo(
                    dependencyPlugin(), 
                    goal("unpack"), 
                    executionConfiguration(projectVersion, outputDirectory), 
                    executionEnvironment(ctx.getProject(), ctx.getSession(), this.pluginManager));
            // @formatter:on
        } catch (MojoExecutionException e) {
            Throwables.propagate(e);
        }
        return findFile(outputDirectory, ctx.getLicenseFileName());
    }

    private Xpp3Dom executionConfiguration(String projectVersion, Path outputDirectory) {
        // @formatter:off
        return MojoExecutor.configuration(
              element("artifactItems", element("artifactItem",
                      element("groupId", ctx.getProject().getGroupId()),
                      element("artifactId", ctx.getProject().getArtifactId()),
                      element("version", projectVersion),
                      element("type", "tar.gz"),
                      element("overWrite", String.valueOf(true)))),
              element("outputDirectory", String.valueOf(outputDirectory)),
              element("overWriteReleases", String.valueOf(true))
      );
      // @formatter:off
    }

    private static Plugin dependencyPlugin() {
        return plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.10"));
    };
    
    private static File findFile(Path base, final String fileName) {
        final List<File> files = new ArrayList<File>();
        try {
            Files.walkFileTree(base, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult visitFile(Path currentPath, BasicFileAttributes attrs) throws IOException {
                    File currentFile = currentPath.toFile();
                    if(currentFile.getName().equals(fileName)) {
                        files.add(currentFile);
                        return FileVisitResult.TERMINATE;
                    }
                    return super.visitFile(currentPath, attrs);
                }
                
            });
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return Iterables.getLast(files, null);
    }
    
}
