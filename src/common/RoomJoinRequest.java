package common;

public final class RoomJoinRequest implements ChatMessage {
    private final String from;
    private final String roomId;

    public RoomJoinRequest(String from, String roomId) {
        this.from = from;
        this.roomId = roomId;
    }
    @Override public String type() { return "room_join"; }
    @Override public String from() { return from; }
    public String roomId() { return roomId; }
}
