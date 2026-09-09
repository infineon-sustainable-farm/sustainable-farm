package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;

/**
 * Builds the subject and HTML body of booking confirmation / reminder emails.
 */
public final class BookingEmailBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private BookingEmailBuilder() {
    }

    public static MailContent confirmation(Booking booking) {
        String subject = "Confirmation de votre réservation " + reference(booking);
        return new MailContent(subject, buildEmail(booking,
                "Votre réservation est confirmée",
                "Merci pour votre confiance. Votre réservation "
                        + reference(booking) + " a été confirmée.",
                false));
    }

    public static MailContent reminder(Booking booking) {
        String subject = "Rappel: votre activité " + booking.getActivity().getName()
                + " a lieu demain";
        return new MailContent(subject, buildEmail(booking,
                "Rappel de votre activité",
                "Votre activité a lieu dans moins de 24 heures. Nous vous attendons!",
                true));
    }

    private static String buildEmail(Booking booking, String title, String intro, boolean reminder) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;"
                + "border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;\">");
        html.append("<div style=\"background:#1a5d1f;color:#fff;padding:16px 24px;\">");
        html.append("<h2 style=\"margin:0;\">").append(HtmlUtils.htmlEscape(title)).append("</h2>");
        html.append("</div>");
        html.append("<div style=\"padding:24px;\">")
                .append("<p>").append(HtmlUtils.htmlEscape(intro)).append("</p>")
                .append("<table style=\"border-collapse:collapse;width:100%;font-size:14px;\">")
                .append(row("Référence", reference(booking)))
                .append(row("Activité", booking.getActivity().getName()))
                .append(row("Date", booking.getTimeSlot().getDate().format(DATE)))
                .append(row("Créneau", booking.getTimeSlot().getStartTime().format(TIME)
                        + " - " + booking.getTimeSlot().getEndTime().format(TIME)))
                .append(row("Personnes", String.valueOf(booking.getPeopleCount())))
                .append(row("Total", booking.getActivity().getPrice()
                        .multiply(java.math.BigDecimal.valueOf(booking.getPeopleCount()))
                        .toPlainString() + " FCFA"))
                .append("</table>");
        if (!reminder) {
            html.append("<p style=\"margin-top:16px;font-size:13px;color:#64748b;\">Paiement à "
                    + "l'arrivée (espèces, Orange Money ou Moov Money).</p>");
        }
        html.append("</div></div>");
        return html.toString();
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"border:1px solid #e2e8f0;padding:8px;color:#475569;\">"
                + HtmlUtils.htmlEscape(label) + "</td>"
                + "<td style=\"border:1px solid #e2e8f0;padding:8px;font-weight:600;\">"
                + HtmlUtils.htmlEscape(value) + "</td></tr>";
    }

    private static String reference(Booking booking) {
        String id = booking.getId() == null ? "" : String.format("%05d", booking.getId());
        return "BK-" + id;
    }

    public record MailContent(String subject, String html) {
    }
}