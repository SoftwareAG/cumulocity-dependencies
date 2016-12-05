package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;
import com.google.common.base.Throwables;

@Component(role = PreviousProjectVersionProvider.class)
public class PreviousProjectVersionProviderImpl implements PreviousProjectVersionProvider {
    
    @Requirement
    private LicensePluginContext ctx;
    
    @Requirement
    private BuildPluginManager pluginManager;

    @Override
    public String get() {
        Path outputFile = Paths.get(ctx.getProject().getBuild().getDirectory(), "versionsReport.txt");
        try {
            // @formatter:off
            executeMojo(
                    versionPlugin(), 
                    goal("display-dependency-updates"), 
                    executionConfiguration(outputFile),
                    executionEnvironment());
            // @formatter:on
        } catch (MojoExecutionException ex) {
            Throwables.propagate(ex);
        }
        String reportText = getReportText(outputFile);
        return parseVersion(reportText);
    }

    private Xpp3Dom executionConfiguration(Path outputFile) {
        // @formatter:off
        return MojoExecutor.configuration(
                element("allowSnapshots", String.valueOf(false)), 
                element("outputFile", String.valueOf(outputFile)),
                element("outputEncoding", "UTF-8")
        );
        // @formatter:on
    }

    private static Plugin versionPlugin() {
        return plugin(groupId("org.codehaus.mojo"), artifactId("versions-maven-plugin"), version("2.3"));
    }

    private String parseVersion(String reportText) {
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

    private String getReportText(Path outputFile) {
        if (Files.exists(outputFile)) {
            try {
                return FileUtils.readFileToString(outputFile.toFile(), "UTF-8");
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        return null;
    }

    private MavenSession newSession(MavenProject projectCopy) {
        MavenSession sessionCopy = ctx.getSession().clone();
        sessionCopy.setCurrentProject(projectCopy);
        return sessionCopy;
    }
    
    private ExecutionEnvironment executionEnvironment() {
        List<Dependency> dependencies = newDependencies();
        MavenProject project = newProject(dependencies);
        MavenSession session = newSession(project);
        return MojoExecutor.executionEnvironment(project, session, pluginManager);
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

}
