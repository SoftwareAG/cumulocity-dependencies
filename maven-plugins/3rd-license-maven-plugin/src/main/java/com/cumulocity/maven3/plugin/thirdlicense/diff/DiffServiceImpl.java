package com.cumulocity.maven3.plugin.thirdlicense.diff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;
import com.google.common.base.Throwables;

@Component( role = DiffService.class )
public class DiffServiceImpl implements DiffService {

    @Requirement
    private LicensePluginContext ctx;
    
    @Requirement
    private BuildPluginManager pluginManager;
    
    @Requirement
    private PreviousProjectVersionProvider previousProjectVersionProvider;
    
    @Requirement
    private ProjectLicenseProvider projectLicenseProvider;
    
    @Requirement
    private EmailSender emailSender;

    @Override
    public void execute() throws MojoExecutionException {
        String previousVersion = previousProjectVersionProvider.get();
        if (previousVersion == null) {
            ctx.info("No previous released version of current artifact found!");
            return;
        } else {
            ctx.info("Previous released version of current artifact is: " + previousVersion);
        }
        File previousLicenseFile = projectLicenseProvider.get(previousVersion);
        if (previousLicenseFile == null) {
            ctx.info("No previous third party license file in previous artifact!");
            return;
        } else {
            ctx.info("Previous third party license file found: " + previousLicenseFile.getAbsolutePath());
        }
        File diff = new File(ctx.getLicenseFilePath(), ctx.getLicenseFileName() + "_diff");
        createDiffFile(ctx.getLicenseFilePath(), previousLicenseFile, diff);
        ctx.info("Diff of third party license files stored: " + diff);
        emailSender.sendDiff(diff);
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
