package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static com.google.common.base.Throwables.propagate;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;

@Component(role = EmailSender.class)
public class EmailSenderImpl implements EmailSender {
    
    @Requirement
    private LicensePluginContext ctx;
    
    @Override
    public void sendDiff(File diffFile) {
        String diff = asString(diffFile);
        ctx.info("Sending diff " + diff);

        System.out.println(ctx.getSettingsProperties().getProperty("abc"));
    }

    private String asString(File diffFile) {
        try {
            return FileUtils.readFileToString(diffFile);
        } catch (IOException e) {
            throw propagate(e);
        }
    }
    
    
    
    

}
