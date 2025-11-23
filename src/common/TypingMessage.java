package common;

import java.io.Serializable;

public final class TypingMessage implements ChatMessage, Serializable {
    private final String from;
    private final boolean typing; // true=입력중, false=중지
    private final long ttlMs;     // 표시 유지 시간(예: 2000ms)

    public TypingMessage(String from, boolean typing, long ttlMs) {
        this.from = from;
        this.typing = typing;
        this.ttlMs = ttlMs;
    }

    @Override public String type() { return "typing"; }
    @Override public String from() { return from; }
    public boolean typing() { return typing; }
    public long ttlMs() { return ttlMs; }
}
