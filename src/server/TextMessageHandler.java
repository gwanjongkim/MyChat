// File: src/server/TextMessageHandler.java
// =========================
package server;
import common.TextMessage;

public final class TextMessageHandler implements MessageHandler<TextMessage> {
    @Override public void handle(TextMessage msg, ClientContext from) {
        from.server().broadcast(msg, from);
    }
}
