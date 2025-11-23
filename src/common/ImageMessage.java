// File: src/common/ImageMessage.java
// =========================
package common;

public final class ImageMessage implements ChatMessage {
    private static final long serialVersionUID = 1L;
    private final String from;
    private final byte[] data;       // raw image bytes (PNG/JPG/GIF)
    private final String mime;       // e.g., "image/png"
    private final int widthHint;     // optional display hint
    private final int heightHint;    // optional display hint

    public ImageMessage(String from, byte[] data, String mime, int w, int h) {
        this.from = from;
        this.data = data;
        this.mime = mime;
        this.widthHint = w;
        this.heightHint = h;
    }

    @Override public String type() { return "image"; }
    @Override public String from() { return from; }
    public byte[] data() { return data; }
    public String mime() { return mime; }
    public int widthHint() { return widthHint; }
    public int heightHint() { return heightHint; }
}

