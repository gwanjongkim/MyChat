package server;

import common.TypingMessage;

public final class TypingMessageHandler implements MessageHandler<TypingMessage> {
    @Override
    public void handle(TypingMessage msg, ClientContext ctx) {

        ctx.server().broadcastScoped(ctx, msg);
    }
}
