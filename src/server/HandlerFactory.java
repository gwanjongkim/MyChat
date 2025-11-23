// File: src/server/HandlerFactory.java
// Factory: returns handler by message type
// =========================
package server;
import common.ChatMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class HandlerFactory {
    private final Map<String, MessageHandler<?>> handlers = new ConcurrentHashMap<>();

    public <T extends ChatMessage> void register(String type, MessageHandler<T> handler) {
        handlers.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends ChatMessage> MessageHandler<T> get(String type) {
        return (MessageHandler<T>) handlers.get(type);
    }
}
