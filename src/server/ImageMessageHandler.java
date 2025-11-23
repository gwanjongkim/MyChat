// File: src/server/ImageMessageHandler.java
// =========================
package server;
import common.ImageMessage;

public final class ImageMessageHandler implements MessageHandler<ImageMessage> {
    @Override public void handle(ImageMessage msg, ClientContext from) {
        from.server().broadcast(msg, from);
    }
}
