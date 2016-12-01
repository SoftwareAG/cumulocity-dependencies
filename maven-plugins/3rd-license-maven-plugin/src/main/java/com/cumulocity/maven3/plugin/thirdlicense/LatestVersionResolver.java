package com.cumulocity.maven3.plugin.thirdlicense;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

public class LatestVersionResolver {

    public static void main(String[] args) throws MojoExecutionException {
        new LatestVersionResolver().fire();
    }

    private void fire() throws MojoExecutionException {
        Plugin versionsPlugin = plugin(groupId("org.codehaus.mojo"), artifactId("versions-maven-plugin"), version("2.3"));

        MavenProject mavenProject = new MavenProject();
        
        ExecutionEnvironment env = executionEnvironment(mavenProject, null, null);
        
        executeMojo(versionsPlugin, goal("dependency-updates-report"), configuration(), env);
    }

}
