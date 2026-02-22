package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    public MailService(
        JHipsterProperties jHipsterProperties,
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(jHipsterProperties.getMail().getFrom());
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        sendEmailFromTemplateSync(user, templateName, titleKey);
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }

    // ── Booking notification emails ─────────────────────────────────────

    private static final Map<String, String> STATUS_STYLES = Map.of(
        "CONFIRME",
        "background-color: #d1fae5; color: #065f46;",
        "REFUSE",
        "background-color: #fee2e2; color: #991b1b;",
        "ANNULE",
        "background-color: #fee2e2; color: #991b1b;",
        "EFFECTUE",
        "background-color: #dbeafe; color: #1e40af;",
        "EN_ATTENTE",
        "background-color: #fef3c7; color: #92400e;"
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Send a booking notification email (async).
     *
     * @param recipientEmail the recipient email address
     * @param recipientName  the recipient first name (for greeting)
     * @param booking        the booking (with trajet loaded)
     * @param ride           the ride
     * @param action         the action key: ACCEPTED, REJECTED, CANCELLED, COMPLETED, NEW_BOOKING, RIDE_CANCELLED
     * @param isDriver       true if recipient is the driver, false if passenger
     */
    @Async
    public void sendBookingNotificationEmail(
        String recipientEmail,
        String recipientName,
        Booking booking,
        Ride ride,
        String action,
        boolean isDriver
    ) {
        if (recipientEmail == null) {
            LOG.debug("Cannot send booking notification: recipient email is null");
            return;
        }

        LOG.debug("Sending booking notification email to '{}' for action '{}'", recipientEmail, action);

        Locale locale = Locale.FRENCH;
        String baseUrl = jHipsterProperties.getMail().getBaseUrl();

        String subject = messageSource.getMessage("email.booking." + action + ".subject", null, locale);
        String headerSubtitle = messageSource.getMessage("email.booking." + action + ".header", null, locale);
        String mainMessage = resolveMainMessage(action, isDriver, recipientName, booking, ride, locale);
        String statusLabel = messageSource.getMessage("email.booking." + action + ".status", null, locale);
        String statusStyle = STATUS_STYLES.getOrDefault(resolveStatusKey(action), STATUS_STYLES.get("EN_ATTENTE"));

        String heureDepart = ride.getHeureDepart() + "h" + ride.getMinuteDepart();
        String montant = booking.getMontantTotal() != null ? String.format("%.0f FCFA", booking.getMontantTotal()) : "N/A";

        Context context = new Context(locale);
        context.setVariable("subject", subject);
        context.setVariable("headerSubtitle", headerSubtitle);
        context.setVariable("statusLabel", statusLabel);
        context.setVariable("statusStyle", statusStyle);
        context.setVariable("greeting", messageSource.getMessage("email.booking.greeting", new Object[] { recipientName }, locale));
        context.setVariable("mainMessage", mainMessage);
        context.setVariable("villeDepart", ride.getVilleDepart());
        context.setVariable("villeArrivee", ride.getVilleArrivee());
        context.setVariable("lieuDitDepart", ride.getLieuDitDepart());
        context.setVariable("lieuDitArrivee", ride.getLieuDitArrivee());
        context.setVariable("dateDepart", ride.getDateDepart().format(DATE_FORMATTER));
        context.setVariable("heureDepart", heureDepart);
        context.setVariable("nbPlaces", String.valueOf(booking.getNbPlacesReservees()));
        context.setVariable("montant", montant);
        // For COMPLETED action, passengers get a link to rate the driver on the trip page
        String actionUrl;
        String actionLabelKey;
        if ("COMPLETED".equals(action) && !isDriver) {
            actionUrl = baseUrl + "/trip/" + ride.getId();
            actionLabelKey = "email.booking.action.rateDriver";
        } else if (isDriver) {
            actionUrl = baseUrl + "/my-trips";
            actionLabelKey = "email.booking.action.viewTrips";
        } else {
            actionUrl = baseUrl + "/bookings";
            actionLabelKey = "email.booking.action.viewBookings";
        }
        context.setVariable("actionUrl", actionUrl);
        context.setVariable("actionLabel", messageSource.getMessage(actionLabelKey, null, locale));

        String content = templateEngine.process("mail/bookingNotificationEmail", context);
        sendEmailSync(recipientEmail, subject, content, false, true);
    }

    private String resolveMainMessage(String action, boolean isDriver, String name, Booking booking, Ride ride, Locale locale) {
        String route = ride.getVilleDepart() + " → " + ride.getVilleArrivee();
        String key = "email.booking." + action + (isDriver ? ".message.driver" : ".message.passenger");
        return messageSource.getMessage(key, new Object[] { route }, locale);
    }

    private String resolveStatusKey(String action) {
        return switch (action) {
            case "ACCEPTED" -> "CONFIRME";
            case "REJECTED" -> "REFUSE";
            case "CANCELLED", "RIDE_CANCELLED" -> "ANNULE";
            case "COMPLETED" -> "EFFECTUE";
            case "NEW_BOOKING" -> "EN_ATTENTE";
            default -> "EN_ATTENTE";
        };
    }
}
