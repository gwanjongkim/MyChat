package server;

import common.ChatMessage;
import common.TextMessage;
import common.ImageMessage;
import common.TypingMessage;
import common.MoveMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class HandlerFactory {

    private final Map<String, MessageHandler<?>> handlers = new ConcurrentHashMap<>();

    public HandlerFactory() {

        // 기본 메시지 핸들러 등록
        register("text",   new TextMessageHandler());
        register("image",  new ImageMessageHandler());
        register("typing", new TypingMessageHandler());

        // ★ 신규 추가: 체스 말 이동 메시지 핸들러
        register("move",   new MoveMessageHandler());
    }

    public <T extends ChatMessage> void register(String type, MessageHandler<T> handler) {
        handlers.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends ChatMessage> MessageHandler<T> get(String type) {
        return (MessageHandler<T>) handlers.get(type);
    }
}
