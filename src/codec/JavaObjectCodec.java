// File: src/codec/JavaObjectCodec.java
// Strategy: Java Object Serialization (pluggable)
// =========================
package codec;
import common.ChatMessage;
import java.io.*;

public final class JavaObjectCodec implements MessageCodec {
    @Override public byte[] encode(ChatMessage m) throws IOException {
        try (var baos = new ByteArrayOutputStream();
             var oos  = new ObjectOutputStream(baos)) {
            oos.writeObject(m);
            oos.flush();
            return baos.toByteArray();
        }
    }

    @Override public ChatMessage decode(byte[] buf) throws IOException, ClassNotFoundException {
        try (var bais = new ByteArrayInputStream(buf);
             var ois  = new ObjectInputStream(bais)) {
            return (ChatMessage) ois.readObject();
        }
    }
}
