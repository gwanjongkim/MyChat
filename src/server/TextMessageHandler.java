// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package server;

import common.TextMessage;

public final class TextMessageHandler implements MessageHandler<TextMessage> {
    public TextMessageHandler() {
    }

    public void handle(TextMessage var1, ClientContext var2) {
        var2.server().broadcastScoped(var2, var1);
    }
}
