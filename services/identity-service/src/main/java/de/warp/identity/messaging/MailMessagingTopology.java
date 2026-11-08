// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.messaging;

public final class MailMessagingTopology {

    public static final String MAIL_EXCHANGE = "warp.mail";
    public static final String MAIL_QUEUE = "warp.mail.send";
    public static final String MAIL_ROUTING_KEY = "mail.send";

    public static final String DEAD_LETTER_EXCHANGE = "warp.mail.dlx";
    public static final String DEAD_LETTER_QUEUE = "warp.mail.send.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "mail.send.dead";

    private MailMessagingTopology() {
    }
}