package server;

import common.TypingMessage;

public final class TypingMessageHandler implements MessageHandler<TypingMessage> {
    @Override
    public void handle(TypingMessage msg, ClientContext ctx) {
        // 단순 중계만 (보낸 사람 포함 모두에게 보내도 무방)
        ctx.server().broadcast(msg, ctx);
    }
}
