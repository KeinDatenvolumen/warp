// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
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