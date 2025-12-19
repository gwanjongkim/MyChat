package common;

public final class RoomJoined implements ChatMessage {
    private final String from;   // "system"
    private final String roomId;
    private final int myColor;   // GamePanel.WHITE/BLACK
    private final String whiteName;
    private final String blackName;

    public RoomJoined(String roomId, int myColor, String whiteName, String blackName) {
        this.from = "system";
        this.roomId = roomId;
        this.myColor = myColor;
        this.whiteName = whiteName;
        this.blackName = blackName;
    }
    @Override public String type() { return "room_joined"; }
    @Override public String from() { return from; }

    public String roomId() { return roomId; }
    public int myColor() { return myColor; }
    public String whiteName() { return whiteName; }
    public String blackName() { return blackName; }
}
