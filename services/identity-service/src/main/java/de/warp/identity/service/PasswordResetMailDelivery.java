// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Service
public class PasswordResetMailDelivery {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String publicUrl;

    public PasswordResetMailDelivery(
            JavaMailSender mailSender,
            @Value("${warp.mail.from}") String mailFrom,
            @Value("${warp.frontend.public-url}") String publicUrl
    ) {
        this.mailSender = Objects.requireNonNull(
                mailSender,
                "mailSender darf nicht null sein"
        );
        this.mailFrom = requireText(
                mailFrom,
                "mailFrom"
        );
        this.publicUrl = removeTrailingSlashes(
                requireText(
                        publicUrl,
                        "publicUrl"
                )
        );
    }

    public void sendPasswordResetEmail(
            UserEntity user,
            String plainToken
    ) {
        Objects.requireNonNull(
                user,
                "user darf nicht null sein"
        );

        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        String resetLink = createResetLink(plainToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject(
                "WARP-Passwort zurücksetzen"
        );
        message.setText(
                createMessageText(
                        user.getName(),
                        resetLink
                )
        );

        mailSender.send(message);
    }

    String createResetLink(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        return UriComponentsBuilder
                .fromUriString(publicUrl)
                .path("/reset-password")
                .queryParam("token", plainToken)
                .build()
                .encode()
                .toUriString();
    }

    private String createMessageText(
            String name,
            String resetLink
    ) {
        return """
                Hallo %s,

                über den folgenden Link kannst du ein neues Passwort für dein WARP-Konto vergeben:

                %s

                Der Link ist 30 Minuten gültig und kann nur einmal verwendet werden.

                Falls du keine Passwort-Rücksetzung angefordert hast, kannst du diese E-Mail ignorieren. Dein bisheriges Passwort bleibt unverändert.

                Viele Grüße
                dein WARP-Team
                """.formatted(name, resetLink);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " darf nicht leer sein"
            );
        }

        return value;
    }

    private static String removeTrailingSlashes(
            String value
    ) {
        String result = value;

        while (result.endsWith("/")) {
            result = result.substring(
                    0,
                    result.length() - 1
            );
        }

        return result;
    }
}