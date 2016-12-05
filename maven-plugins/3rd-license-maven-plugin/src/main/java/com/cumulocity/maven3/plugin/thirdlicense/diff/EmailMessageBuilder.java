package com.cumulocity.maven3.plugin.thirdlicense.diff;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.base.Throwables;

public class EmailMessageBuilder {

    private String from;
    private String to;
    private String text;
    private String subject;
    private File attachement;
    private final Session session;

    public static EmailMessageBuilder aMessage(Session session) {
        return new EmailMessageBuilder(session);
    };

    public EmailMessageBuilder(Session session) {
        this.session = session;
    }

    public EmailMessageBuilder withFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * Colon separated list allowed
     */
    public EmailMessageBuilder withTo(String to) {
        this.to = to;
        return this;
    }

    public EmailMessageBuilder withBody(String text) {
        this.text = text;
        return this;
    }

    public EmailMessageBuilder withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public EmailMessageBuilder withAttachement(File attachement) {
        this.attachement = attachement;
        return this;
    }

    public Message build() {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            Multipart multipart = new MimeMultipart();
            message.setContent(multipart);
            multipart.addBodyPart(bodyText());
            if (attachement != null) {
                multipart.addBodyPart(bodyAttachment());
            }
            return message;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private BodyPart bodyAttachment() throws MessagingException {
        BodyPart bodyAttachement = new MimeBodyPart();
        DataSource source = new FileDataSource(attachement.getAbsolutePath());
        bodyAttachement.setDataHandler(new DataHandler(source));
        bodyAttachement.setFileName(attachement.getAbsolutePath());
        return bodyAttachement;
    }

    private BodyPart bodyText() throws MessagingException {
        BodyPart bodyText = new MimeBodyPart();
        bodyText.setText(text);
        return bodyText;
    }

}
