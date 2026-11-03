package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Service
public class VerificationMailDelivery {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String publicUrl;

    public VerificationMailDelivery(
            JavaMailSender mailSender,
            @Value("${warp.mail.from}") String mailFrom,
            @Value("${warp.frontend.public-url}") String publicUrl
    ) {
        this.mailSender = Objects.requireNonNull(
                mailSender,
                "mailSender darf nicht null sein"
        );
        this.mailFrom = requireText(mailFrom, "mailFrom");
        this.publicUrl = removeTrailingSlashes(
                requireText(publicUrl, "publicUrl")
        );
    }

    public void sendVerificationEmail(
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

        String confirmationLink = createConfirmationLink(
                plainToken
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject(
                "E-Mail-Adresse für WARP bestätigen"
        );
        message.setText(
                createMessageText(
                        user.getName(),
                        confirmationLink
                )
        );

        mailSender.send(message);
    }

    String createConfirmationLink(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        return UriComponentsBuilder
                .fromUriString(publicUrl)
                .path("/verify-email")
                .queryParam("token", plainToken)
                .build()
                .encode()
                .toUriString();
    }

    private String createMessageText(
            String name,
            String confirmationLink
    ) {
        return """
                Hallo %s,

                bitte bestätige deine E-Mail-Adresse über den folgenden Link:

                %s

                Der Link ist 24 Stunden gültig und kann nur einmal verwendet werden.

                Falls du dich nicht bei WARP registriert hast, kannst du diese E-Mail ignorieren.

                Viele Grüße
                dein WARP-Team
                """.formatted(name, confirmationLink);
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

    private static String removeTrailingSlashes(String value) {
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