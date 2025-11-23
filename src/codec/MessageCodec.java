// File: src/codec/MessageCodec.java
// =========================
package codec;
import common.ChatMessage;
import java.io.IOException;

public interface MessageCodec {
    byte[] encode(ChatMessage m) throws IOException;
    ChatMessage decode(byte[] buf) throws IOException, ClassNotFoundException;
}
