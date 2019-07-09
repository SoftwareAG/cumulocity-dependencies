package com.cumulocity.maven3.plugin.thirdlicense;

import com.cumulocity.maven3.plugin.thirdlicense.artifact.Artifacts;
import com.cumulocity.maven3.plugin.thirdlicense.fetcher.Build;
import com.cumulocity.maven3.plugin.thirdlicense.fetcher.FetcherDependency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.internal.util.Sets;
import lombok.SneakyThrows;

import okhttp3.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;


@Mojo(name = "3rd-tpp-fetcher-scan", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TppFetcherScanMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "http://localhost:8080", property = "tpp.fetcher.url")
    private String tppFetcherUrl;

    @Parameter(required = true, property = "tpp.fetcher.project.name")
    private String tppFetcherProjectName;

    @Parameter(property = "tpp.fetcher.ignored.modules")
    private List<String> tppFetcherIgnoredModules;

    @Parameter(property = "tpp.fetcher.scan.enabled", defaultValue = "true")
    private Boolean tppFetcherScanEnabled;

    private ObjectMapper mapper = new ObjectMapper();
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void execute() {
        if (!tppFetcherScanEnabled || isIgnored(mavenProject.getArtifactId())) {
            return;
        }
        getLog().info("Fetching dependency information for module: " + mavenProject.getArtifactId() + ", project: " + tppFetcherProjectName);
        Set<FetcherDependency> dependencies = Sets.newHashSet();
        for (final Artifact artifact : (Set<Artifact>)mavenProject.getArtifacts()) {
            if (Artifacts.isCumulocityArtifact(artifact.getGroupId())) {
              getLog().debug("Internal dependency will be ignored " + artifact.getGroupId() + ":" + artifact.getArtifactId());
              continue;
            }
            getLog().debug(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
            FetcherDependency fetcherDependency = new FetcherDependency();
            fetcherDependency.setName(artifact.getGroupId() + ":" + artifact.getArtifactId());
            fetcherDependency.setVersion(artifact.getVersion());
            dependencies.add(fetcherDependency);
        }
        Build build = new Build();
        build.setDependencies(dependencies);
        build.setRevision(mavenProject.getVersion());
        getLog().debug("Build dependencies: " + build.toString());
        callTppFetcher(build);
    }

    @SneakyThrows
    private void callTppFetcher(Build build) {
        String json = mapper.writeValueAsString(build);
        MediaType JSON = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                    .url(tppFetcherUrl + "/api/projects/" + tppFetcherProjectName + "/build")
                    .post(requestBody)
                    .build();
       try (Response response = client.newCall(request).execute()) {
           if (!response.isSuccessful()) {
               throw new RuntimeException("Could not create request! " + response.toString());
           }
       }
    }

    private boolean isIgnored(String moduleName) {
        return isNotEmpty(tppFetcherIgnoredModules) && tppFetcherIgnoredModules.contains(moduleName);
    }
}
