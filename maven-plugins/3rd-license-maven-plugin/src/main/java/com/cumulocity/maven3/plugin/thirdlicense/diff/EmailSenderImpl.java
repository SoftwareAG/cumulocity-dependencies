package com.cumulocity.maven3.plugin.thirdlicense.diff;

import static com.cumulocity.maven3.plugin.thirdlicense.context.LicensePluginContext.PROPERTY_KEY_PREFIX;
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

    @Requirement
    private LicensePluginContext ctx;

    @Override
    public void sendDiff(File diffFile) {
        String from = ctx.getSettingsProperties().getProperty(prefixKey("mail.from"));
        String to = ctx.getSettingsProperties().getProperty(prefixKey("mail.to"));
        boolean changesDetected = changesDetected(diffFile);

        // @formatter:off
        Message message = aMessage(getSession())
                .withFrom(from)
                .withTo(to)
                .withSubject(prepareSubject(changesDetected))
                .withBody(prepareBody(changesDetected))
                .withAttachement(prepareAttachment(diffFile, changesDetected))
                .build();
        // @formatter:on

        try {
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
        result.append("\nRegards \n3rd license plugin");
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
        Properties properties = ctx.getSettingsProperties();
        final String username = properties.getProperty(prefixKey("mail.username"));
        final String password = properties.getProperty(prefixKey("mail.password"));
        return Session.getInstance(smtpProperties(), new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties smtpProperties() {
        Properties smtpProperties = new Properties();
        for (String key : ctx.getSettingsProperties().stringPropertyNames()) {
            if (key.startsWith(prefixKey("mail.smtp"))) {
                String smtpKey = unPrefixKey(key);
                String smtpValue = ctx.getSettingsProperties().getProperty(key);
                smtpProperties.setProperty(smtpKey, smtpValue);
            }
        }
        return smtpProperties;
    }

    private static String prefixKey(String key) {
        return String.format("%s.%s", PROPERTY_KEY_PREFIX, key);
    }

    private static String unPrefixKey(String key) {
        return key.substring(PROPERTY_KEY_PREFIX.length());
    }

}
