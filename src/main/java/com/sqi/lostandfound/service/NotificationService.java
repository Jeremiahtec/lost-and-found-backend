package com.sqi.lostandfound.service;

import com.sqi.lostandfound.model.LostItem;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${twilio.account.sid}") private String twilioSid;
    @Value("${twilio.auth.token}")  private String twilioToken;
    @Value("${twilio.whatsapp.from}") private String whatsappFrom;
    @Value("${spring.mail.username}") private String gmailAddress;
    @Value("${app.admin.email}")    private String adminEmail;
    @Value("${app.admin.whatsapp:}") private String adminWhatsapp;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyOnClaim(LostItem item, String claimedBy, String claimContact) {
        String itemName     = item.getItemName();
        String reportedBy   = item.getReportedBy() != null ? item.getReportedBy() : "the reporter";
        String reporterContact = item.getReporterContact();
        String branch       = item.getBranch() != null ? item.getBranch() : "Unknown Branch";

        // ── Message to reporter ──────────────────────────────────────
        String reporterMsg = String.format(
                "Hello %s! 🎉 Great news — your lost item *%s* has been found at SQI %s.\n\n" +
                        "Found by: %s\nContact: %s\n\n" +
                        "Please reach out to them to arrange pickup. — SQI Lost & Found",
                reportedBy, itemName, branch, claimedBy, claimContact
        );

        // ── Message to admin ─────────────────────────────────────────
        String adminMsg = String.format(
                "📦 *SQI Lost & Found — Claim Alert*\n\n" +
                        "Item: *%s*\nBranch: %s\n\n" +
                        "Reporter: %s\nClaimer: %s\nClaimer Contact: %s\n\n" +
                        "Please verify and facilitate handover.",
                itemName, branch, reportedBy, claimedBy, claimContact
        );

        // ── Send to reporter ─────────────────────────────────────────
        if (reporterContact != null && !reporterContact.isBlank()) {
            if (isPhone(reporterContact)) {
                sendWhatsApp("whatsapp:+" + sanitizePhone(reporterContact), reporterMsg);
            } else {
                sendEmail(reporterContact, "Your lost item was found! — SQI Lost & Found", reporterMsg);
            }
        }

        // ── Send to admin ─────────────────────────────────────────────
        sendEmail(adminEmail, "Claim Alert: " + itemName + " — SQI Lost & Found", adminMsg);
        if (adminWhatsapp != null && !adminWhatsapp.isBlank()) {
            sendWhatsApp("whatsapp:+" + sanitizePhone(adminWhatsapp), adminMsg);
        }
    }

    private void sendWhatsApp(String to, String body) {
        try {
            Twilio.init(twilioSid, twilioToken);
            Message.creator(new PhoneNumber(to), new PhoneNumber(whatsappFrom), body).create();
            log.info("WhatsApp sent to {}", to);
        } catch (Exception e) {
            log.error("WhatsApp failed to {}: {}", to, e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(gmailAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body.replace("\n", "<br>"), true);
            mailSender.send(msg);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Email failed to {}: {}", to, e.getMessage());
        }
    }

    private boolean isPhone(String contact) {
        return contact.matches(".*\\d{7,}.*");
    }

    private String sanitizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}