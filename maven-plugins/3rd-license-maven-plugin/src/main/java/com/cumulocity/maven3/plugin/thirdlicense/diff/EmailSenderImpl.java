package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static com.cumulocity.maven3.plugin.thirdlicense.diff.EmailMessageBuilder.aMessage;
import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext;

@Component(role = EmailSender.class)
public class EmailSenderImpl implements EmailSender {

    private static final String PROPERTY_MAIL_TO = "mail.to";
    private static final String PROPERTY_MAIL_PASSWORD = "mail.password";
    private static final String PROPERTY_MAIL_USERNAME = "mail.username";
    
    @Requirement
    private LicensePluginContext ctx;

    private boolean validate() {
        return validate(PROPERTY_MAIL_TO) & validate(PROPERTY_MAIL_USERNAME) & validate(PROPERTY_MAIL_PASSWORD);
    }

    private boolean validate(String key) {
        if (ctx.hasProperty(key)) {
            return true;
        }
        ctx.warn("Missing property '" + LicensePluginContext.PROPERTY_KEY_PREFIX + key + "'; skip email sending!");
        return false;
    }

    @Override
    public void sendDiff(File diffFile) {
        if (!validate()) {
            return;
        }
        String to = ctx.getProperty(PROPERTY_MAIL_TO);
        boolean changesDetected = changesDetected(diffFile);

        try {
            // @formatter:off
            Message message = aMessage(getSession())
                    .withFrom("support@cumulocity.com")
                    .withTo(to)
                    .withSubject(prepareSubject(changesDetected))
                    .withBody(prepareBody(changesDetected))
                    .withAttachement(prepareAttachment(diffFile, changesDetected))
                    .build();
            // @formatter:on
            Transport.send(message);
            ctx.info("Diff sent successfully....");
        } catch (MessagingException e) {
            ctx.warn("Diff not sent: " + e.getMessage());
        }
    }

    private String prepareBody(boolean changesDetected) {
        StringBuilder result = new StringBuilder();
        if (changesDetected) {
            result.append("Changes in 3rd party component licenses attached.");
        } else {
            result.append("Changes in 3rd party component licenses not detected.");
        }
        result.append("\nRegards \nCumulocity 3rd-license-maven-plugin");
        return result.toString();
    }

    private boolean changesDetected(File diffFile) {
        String diff = asString(diffFile);
        return !isBlank(diff);
    }

    private File prepareAttachment(File diffFile, boolean changesDetected) {
        return changesDetected ? diffFile : null;
    }

    private String prepareSubject(boolean changesDetected) {
        StringBuilder result = new StringBuilder();
        result.append("3rd licenses for ");
        result.append(ctx.getProject().getGroupId());
        result.append(":");
        result.append(ctx.getProject().getArtifactId());
        if (!changesDetected) {
            result.append(" - no changes");
        }
        return result.toString();
    }

    private String asString(File diffFile) {
        try {
            return FileUtils.readFileToString(diffFile);
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    private Session getSession() {
        final String username = ctx.getProperty(PROPERTY_MAIL_USERNAME);
        final String password = ctx.getProperty(PROPERTY_MAIL_PASSWORD);
        return Session.getInstance(smtpProperties(), new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties smtpProperties() {
        Properties smtpProperties = new Properties();
        for (String key : ctx.getProperties().stringPropertyNames()) {
            if (key.startsWith("mail.smtp.")) {
                smtpProperties.setProperty(key, ctx.getProperty(key));
            }
        }
        return smtpProperties;
    }
}
