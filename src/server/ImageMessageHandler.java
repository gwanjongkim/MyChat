// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package server;

import common.ImageMessage;

public final class ImageMessageHandler implements MessageHandler<ImageMessage> {
    public ImageMessageHandler() {
    }

    public void handle(ImageMessage var1, ClientContext var2) {
        var2.server().broadcastScoped(var2, var1);
    }
}
