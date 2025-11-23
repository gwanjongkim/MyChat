// File: src/common/ChatMessage.java
// =========================
package common;
import java.io.Serializable;

public interface ChatMessage extends Serializable {
    String type();
    String from();
}