package skylinkers.tn.mediconnectbackend.service.AppointmentServices;


import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class Servicemail {

    private JavaMailSender mailSender;

    private String buildEmailTemplate(String title, String content, String color) {
        String logoBase64 = "";
        try {
            ClassPathResource logo = new ClassPathResource("static/logo.png");
            byte[] logoBytes = logo.getInputStream().readAllBytes();
            logoBase64 = java.util.Base64.getEncoder().encodeToString(logoBytes);
        } catch (Exception e) {
            System.out.println("Logo non trouvé : " + e.getMessage());
        }

        String logoHtml = logoBase64.isEmpty() ? "" :
                "<img src='data:image/png;base64," + logoBase64 + "' alt='MediConnect' style='width:90px;height:auto;margin-bottom:20px;'/><br/>";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style> @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;700&display=swap'); </style></head>" +
                "<body style='margin:0;padding:0;background:#f4f7f6;font-family:Inter,Arial,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f7f6;padding:40px 0;'>" +
                "<tr><td align='center'>" +
                "<table width='580' cellpadding='0' cellspacing='0' style='border-radius:20px;overflow:hidden;background:#ffffff;box-shadow:0 10px 30px rgba(0,0,0,0.05);'>" +

                // HEADER - ICI LE CHANGEMENT : Fond très clair pour que l'identité reste douce
                "<tr><td style='background:rgba(47, 164, 169, 0.08);padding:40px;text-align:center;border-bottom:1px solid rgba(47, 164, 169, 0.1);'>" +
                (logoHtml != null ? logoHtml : "") +
                "<h1 style='color:#0f4c54;margin:0;font-size:24px;font-weight:700;letter-spacing:1px;'>MEDICONNECT</h1>" +
                "</td></tr>" +

                // BANDEAU DU TITRE (L'image que tu as envoyée)
                "<tr><td style='padding:20px 40px 0;text-align:center;'>" +
                "<div style='display:inline-block;background:" + color + ";color:#ffffff;padding:10px 25px;border-radius:50px;" +
                "font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:1px;box-shadow:0 4px 12px rgba(47, 164, 169, 0.2);'>" +
                title +
                "</div></td></tr>" +

                "<tr><td style='padding:40px;'>" + content + "</td></tr>" +

                // FOOTER
                "<tr><td style='background:#fcfdfd;padding:30px;text-align:center;border-top:1px solid #f0f0f0;'>" +
                "<p style='color:#9ca3af;font-size:12px;margin:0;'>MediConnect © 2026</p>" +
                "</td></tr>" +

                "</table></td></tr></table></body></html>";
    }


    @Async
    public void sendConfirmationEmailToPatient(String patientEmail, String patientName,
                                               String doctorName, String date, String heure) {
        try {
            String content =
                    "<h2 style='color:#0f4c54;font-size:24px;font-weight:700;margin:0 0 12px;'>Bonjour " + patientName + ",</h2>" +
                            "<p style='color:#6b7280;font-size:15px;line-height:1.8;margin:0 0 32px;font-weight:300;'>" +
                            "Votre demande de rendez-vous a été <strong style='color:#279096;font-weight:600;'>acceptée</strong> par votre médecin. Retrouvez ci-dessous le récapitulatif de votre consultation." +
                            "</p>" +

                            "<div style='background:#f8fafc;border-radius:16px;padding:32px;margin:0 0 32px;border:1px solid #e5e7eb;'>" +
                            "<table width='100%' cellpadding='0' cellspacing='0'>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Médecin</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>Dr. " + doctorName + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Date</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + date + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Heure</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + heure + "</p>" +
                            "</td></tr>" +

                            "</table></div>" +

                            "<p style='color:#9ca3af;font-size:13px;line-height:1.7;margin:0 0 32px;font-style:italic;'>" +
                            "Merci de vous présenter 10 minutes avant l'heure de votre rendez-vous. En cas d'empêchement, pensez à annuler via la plateforme." +
                            "</p>" +

                            "<div style='text-align:center;'>" +
                            "<a href='http://localhost:4200' style='display:inline-block;background:linear-gradient(135deg,#279096,#0f4c54);color:white;" +
                            "padding:16px 48px;border-radius:50px;text-decoration:none;font-weight:600;font-size:14px;" +
                            "letter-spacing:1px;text-transform:uppercase;box-shadow:0 8px 20px rgba(39,144,150,0.35);'>" +
                            "Voir mes rendez-vous" +
                            "</a></div>";

            sendHtmlEmail(patientEmail, "Rendez-vous confirmé — MediConnect",
                    buildEmailTemplate("Rendez-vous Confirmé", content, "#279096"));
        } catch (Exception e) {
            System.out.println("Erreur mail confirmation : " + e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmailToPatient(String patientEmail, String patientName,
                                               String date, String heure) {
        try {
            String content =
                    "<h2 style='color:#0f4c54;font-size:24px;font-weight:700;margin:0 0 12px;'>Bonjour " + patientName + ",</h2>" +
                            "<p style='color:#6b7280;font-size:15px;line-height:1.8;margin:0 0 32px;font-weight:300;'>" +
                            "Votre rendez-vous a été <strong style='color:#ef4444;font-weight:600;'>annulé</strong>. Vous pouvez reprendre un nouveau rendez-vous à tout moment sur notre plateforme." +
                            "</p>" +

                            "<div style='background:#f8fafc;border-radius:16px;padding:32px;margin:0 0 32px;border:1px solid #e5e7eb;'>" +
                            "<table width='100%' cellpadding='0' cellspacing='0'>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Date annulée</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + date + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Heure annulée</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + heure + "</p>" +
                            "</td></tr>" +

                            "</table></div>" +

                            "<div style='text-align:center;'>" +
                            "<a href='http://localhost:4200' style='display:inline-block;background:linear-gradient(135deg,#279096,#0f4c54);color:white;" +
                            "padding:16px 48px;border-radius:50px;text-decoration:none;font-weight:600;font-size:14px;" +
                            "letter-spacing:1px;text-transform:uppercase;box-shadow:0 8px 20px rgba(39,144,150,0.35);'>" +
                            "Prendre un nouveau RDV" +
                            "</a></div>";

            sendHtmlEmail(patientEmail, "Rendez-vous annulé — MediConnect",
                    buildEmailTemplate("Rendez-vous Annulé", content, "#279096"));
        } catch (Exception e) {
            System.out.println("Erreur mail annulation patient : " + e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmailToDoctor(String doctorEmail, String patientName,
                                              String date, String heure) {
        try {
            String content =
                    "<h2 style='color:#0f4c54;font-size:24px;font-weight:700;margin:0 0 12px;'>Bonjour Docteur,</h2>" +
                            "<p style='color:#6b7280;font-size:15px;line-height:1.8;margin:0 0 32px;font-weight:300;'>" +
                            "Le patient <strong style='color:#279096;font-weight:600;'>" + patientName + "</strong> a annulé son rendez-vous. Le créneau est désormais disponible." +
                            "</p>" +

                            "<div style='background:#f8fafc;border-radius:16px;padding:32px;margin:0 0 32px;border:1px solid #e5e7eb;'>" +
                            "<table width='100%' cellpadding='0' cellspacing='0'>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Patient</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + patientName + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Créneau libéré</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + date + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Heure</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + heure + "</p>" +
                            "</td></tr>" +

                            "</table></div>";

            sendHtmlEmail(doctorEmail, "RDV annulé par le patient — MediConnect",
                    buildEmailTemplate("RDV Annulé", content, "#279096"));
        } catch (Exception e) {
            System.out.println("Erreur mail annulation docteur : " + e.getMessage());
        }
    }

    @Async
    public void sendRefusalEmailToPatient(String patientEmail, String patientName,
                                          String doctorName, String date, String heure) {
        try {
            String content =
                    "<h2 style='color:#0f4c54;font-size:24px;font-weight:700;margin:0 0 12px;'>Bonjour " + patientName + ",</h2>" +
                            "<p style='color:#6b7280;font-size:15px;line-height:1.8;margin:0 0 32px;font-weight:300;'>" +
                            "Votre demande de rendez-vous avec <strong style='color:#279096;font-weight:600;'>Dr. " + doctorName + "</strong> n'a malheureusement pas pu être acceptée. Nous vous invitons à choisir un autre créneau." +
                            "</p>" +

                            "<div style='background:#f8fafc;border-radius:16px;padding:32px;margin:0 0 32px;border:1px solid #e5e7eb;'>" +
                            "<table width='100%' cellpadding='0' cellspacing='0'>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Médecin</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>Dr. " + doctorName + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Date refusée</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + date + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Heure</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + heure + "</p>" +
                            "</td></tr>" +

                            "</table></div>" +

                            "<div style='text-align:center;'>" +
                            "<a href='http://localhost:4200' style='display:inline-block;background:linear-gradient(135deg,#279096,#0f4c54);color:white;" +
                            "padding:16px 48px;border-radius:50px;text-decoration:none;font-weight:600;font-size:14px;" +
                            "letter-spacing:1px;text-transform:uppercase;box-shadow:0 8px 20px rgba(39,144,150,0.35);'>" +
                            "Reprendre un RDV" +
                            "</a></div>";

            sendHtmlEmail(patientEmail, "Demande de RDV refusée — MediConnect",
                    buildEmailTemplate("RDV Refusé", content, "#279096"));
        } catch (Exception e) {
            System.out.println("Erreur mail refus : " + e.getMessage());
        }
    }

    @Async
    public void sendDoneEmailToPatient(String patientEmail, String patientName,
                                       String doctorName, String date, String heure) {
        try {
            String content =
                    "<h2 style='color:#0f4c54;font-size:24px;font-weight:700;margin:0 0 12px;'>Bonjour " + patientName + ",</h2>" +
                            "<p style='color:#6b7280;font-size:15px;line-height:1.8;margin:0 0 32px;font-weight:300;'>" +
                            "Votre consultation avec <strong style='color:#279096;font-weight:600;'>Dr. " + doctorName + "</strong> est désormais <strong style='color:#279096;font-weight:600;'>terminée</strong>. Nous espérons que tout s'est bien passé." +
                            "</p>" +

                            "<div style='background:#f8fafc;border-radius:16px;padding:32px;margin:0 0 32px;border:1px solid #e5e7eb;'>" +
                            "<table width='100%' cellpadding='0' cellspacing='0'>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Médecin</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>Dr. " + doctorName + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;border-bottom:1px solid #f0f0f0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Date de consultation</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + date + "</p>" +
                            "</td></tr>" +

                            "<tr><td style='padding:14px 0;'>" +
                            "<p style='color:#9ca3af;font-size:11px;font-weight:600;letter-spacing:2px;text-transform:uppercase;margin:0 0 4px;'>Heure</p>" +
                            "<p style='color:#1f2937;font-size:16px;font-weight:600;margin:0;'>" + heure + "</p>" +
                            "</td></tr>" +

                            "</table></div>" +

                            "<p style='color:#9ca3af;font-size:13px;line-height:1.7;margin:0 0 32px;font-style:italic;'>" +
                            "N'hésitez pas à reprendre un rendez-vous si vous en avez besoin." +
                            "</p>" +

                            "<div style='text-align:center;'>" +
                            "<a href='http://localhost:4200' style='display:inline-block;background:linear-gradient(135deg,#279096,#0f4c54);color:white;" +
                            "padding:16px 48px;border-radius:50px;text-decoration:none;font-weight:600;font-size:14px;" +
                            "letter-spacing:1px;text-transform:uppercase;box-shadow:0 8px 20px rgba(39,144,150,0.35);'>" +
                            "Reprendre un RDV" +
                            "</a></div>";

            sendHtmlEmail(patientEmail, "Consultation terminée — MediConnect",
                    buildEmailTemplate("Consultation Terminée", content, "#279096"));
        } catch (Exception e) {
            System.out.println("Erreur mail done : " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    @Async
    public void sendRescheduleEmailToPatient(String patientEmail, String patientName,
                                             String doctorName, String ancienneHeure, String nouvelleHeure) {
        String colorMain = "#2FA4A9";
        try {
            String content =
                    "<div style='text-align:center; margin-bottom:30px;'>" +
                            "  <h2 style='color:#0f4c54; font-size:22px; font-weight:700; margin:0;'>Bonjour " + patientName + ",</h2>" +
                            "  <p style='color:#6b7280; font-size:15px; margin-top:10px;'>Votre planning a été mis à jour suite à une urgence.</p>" +
                            "</div>" +

                            // CARD DESIGN - Fond légèrement coloré pour faire ressortir le blanc
                            "<div style='background:" + colorMain + "; border-radius:18px; padding:30px; margin-bottom:30px;'>" +
                            "  <table width='100%' cellpadding='0' cellspacing='0'>" +
                            "    <tr>" +
                            "      <td style='padding-bottom:15px; border-bottom:1px solid rgba(255,255,255,0.2);'>" +
                            "        <p style='color:rgba(255,255,255,0.8); font-size:11px; font-weight:600; text-transform:uppercase; margin:0;'>Médecin</p>" +
                            "        <p style='color:#ffffff; font-size:17px; font-weight:600; margin:0;'>Dr. " + doctorName + "</p>" +
                            "      </td>" +
                            "    </tr>" +
                            "    <tr>" +
                            "      <td style='padding:15px 0; border-bottom:1px solid rgba(255,255,255,0.2);'>" +
                            "        <p style='color:rgba(255,255,255,0.8); font-size:11px; font-weight:600; text-transform:uppercase; margin:0;'>Précédemment</p>" +
                            "        <p style='color:rgba(255,255,255,0.6); font-size:15px; text-decoration:line-through; margin:0;'>" + ancienneHeure + "</p>" +
                            "      </td>" +
                            "    </tr>" +
                            "    <tr>" +
                            "      <td style='padding-top:15px;'>" +
                            "        <p style='color:#ffffff; font-size:11px; font-weight:600; text-transform:uppercase; margin:0;'>Nouvel Horaire</p>" +
                            "        <p style='color:#ffffff; font-size:26px; font-weight:800; margin:0;'> " + nouvelleHeure + "</p>" +
                            "      </td>" +
                            "    </tr>" +
                            "  </table>" +
                            "</div>" +

                            "<div style='text-align:center;'>" +
                            "  <a href='http://localhost:4200' style='display:inline-block; background:#0f4c54; color:#ffffff; " +
                            "  padding:15px 35px; border-radius:12px; text-decoration:none; font-weight:600; font-size:14px;'>" +
                            "  Confirmer ma présence" +
                            "  </a>" +
                            "</div>";

            sendHtmlEmail(patientEmail, "🕒 Mise à jour de votre rendez-vous — MediConnect",
                    buildEmailTemplate("Planning Mis à Jour", content, colorMain));

        } catch (Exception e) {
            System.out.println("Erreur mail reschedule : " + e.getMessage());
        }
    }
}
