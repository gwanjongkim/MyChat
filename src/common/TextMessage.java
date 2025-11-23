// File: src/common/TextMessage.java
// =========================
package common;

public final class TextMessage implements ChatMessage {
    private static final long serialVersionUID = 1L;
    private final String from;
    private final String text;

    public TextMessage(String from, String text) {
        this.from = from;
        this.text = text;
    }

    @Override public String type() { return "text"; }
    @Override public String from() { return from; }
    public String text() { return text; }
}

