package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
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

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

@Component( role = DiffService.class )
public class DiffServiceImpl implements DiffService {

    @Requirement
    private LicensePluginContext ctx;
    
    @Requirement
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        String previousVersion = getPreviousVersion();
        if (previousVersion == null) {
            ctx.info("No previous released version of current artifact found!");
            return;
        } else {
            ctx.info("Previous released version of current artifact is: " + previousVersion);
        }
        File previousLicenseFile = getPreviousLicenseFile(previousVersion);
        if (previousLicenseFile == null) {
            ctx.info("No previous third party license file in previous artifact!");
            return;
        } else {
            ctx.info("Previous third party license file found: " + previousLicenseFile.getAbsolutePath());
        }
        File diff = new File(ctx.getLicenseFilePath(), ctx.getLicenseFileName() + "_diff");
        createDiffFile(ctx.getLicenseFilePath(), previousLicenseFile, diff);
        ctx.info("Diff of third party license files stored: " + diff);
        sendDiff(diff);
    }

    private void sendDiff(File diff) {
        // TODO Auto-generated method stub
        
    }

    private String getPreviousVersion() throws MojoExecutionException {
        Plugin versionsPlugin = plugin(groupId("org.codehaus.mojo"), artifactId("versions-maven-plugin"), version("2.3"));
        List<Dependency> dependencies = newDependencies();
        MavenProject projectCopy = newProject(dependencies);
        MavenSession sessionCopy = newSession(projectCopy);
        Path outputFile = Paths.get(ctx.getProject().getBuild().getDirectory(), "versionsReport.txt");
        // @formatter:off
        Xpp3Dom configuration = configuration(
                element("allowSnapshots", String.valueOf(false)), 
                element("outputFile", String.valueOf(outputFile)),
                element("outputEncoding", "UTF-8")
        );
        // @formatter:on
        executeMojo(versionsPlugin, goal("display-dependency-updates"), configuration,
                executionEnvironment(projectCopy, sessionCopy, pluginManager));
        String reportText = null;
        if (Files.exists(outputFile)) {
            try {
                reportText = FileUtils.readFileToString(outputFile.toFile(), "UTF-8");
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        int indexOfGroupId = reportText.indexOf(ctx.getProject().getGroupId());
        if (indexOfGroupId < 0) {
            return null;
        }
        reportText = reportText.substring(indexOfGroupId);
        int indexOfArrow = reportText.indexOf("->");
        if (indexOfArrow < 0) {
            return null;
        }
        return reportText.substring(indexOfArrow + 2).trim();
    }

    private File getPreviousLicenseFile(String previousVersion) throws MojoExecutionException {
        Plugin dependencyPlugin = plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.10"));
        Path outputDirectory = Paths.get(ctx.getProject().getBuild().getDirectory(), "previousReleased");
        ctx.info("Download previous released version to directory " + outputDirectory);
        // @formatter:off
        Xpp3Dom configuration = configuration(
              element("artifactItems", element("artifactItem",
                      element("groupId", ctx.getProject().getGroupId()),
                      element("artifactId", ctx.getProject().getArtifactId()),
                      element("version", previousVersion),
                      element("type", "tar.gz"),
                      element("overWrite", String.valueOf(true)))),
              element("outputDirectory", String.valueOf(outputDirectory)),
              element("overWriteReleases", String.valueOf(true))
      );
      // @formatter:off
      executeMojo(dependencyPlugin, goal("unpack"), configuration, executionEnvironment(ctx.getProject(), ctx.getSession(), this.pluginManager));
      return findFile(outputDirectory, ctx.getLicenseFileName());
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

    private MavenSession newSession(MavenProject projectCopy) {
        MavenSession sessionCopy = ctx.getSession().clone();
        sessionCopy.setCurrentProject(projectCopy);
        return sessionCopy;
    }

    @SuppressWarnings("deprecation")
    private MavenProject newProject(List<Dependency> dependencies) {
        MavenProject projectCopy = new MavenProject(ctx.getProject());
        projectCopy.setParent(null);
        projectCopy.setDependencies(dependencies);
        projectCopy.getDependencyManagement().setDependencies(dependencies);
        return projectCopy;
    }

    private List<Dependency> newDependencies() {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency dependency = new Dependency();
        dependency.setGroupId(ctx.getProject().getGroupId());
        dependency.setArtifactId(ctx.getProject().getArtifactId());
        dependency.setVersion("0.0.0");
        dependencies.add(dependency);
        return dependencies;
    }
    
    private static void createDiffFile(File licenseFile, File previousLicenseFile, File target) {
        String[] command = new String[] { "diff", licenseFile.getAbsolutePath(), previousLicenseFile.getAbsolutePath() };
        try {
            Files.deleteIfExists(target.toPath());
            Process p = Runtime.getRuntime().exec(command);
            Files.copy(p.getInputStream(), target.toPath());
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
