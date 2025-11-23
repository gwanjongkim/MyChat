// File: src/server/MessageHandler.java
// =========================
package server;
import common.ChatMessage;

public interface MessageHandler<T extends ChatMessage> {
    void handle(T msg, ClientContext from);
}
