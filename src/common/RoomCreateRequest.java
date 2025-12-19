package common;

public final class RoomCreateRequest implements ChatMessage {
    private final String from;
    private final String roomName;

    public RoomCreateRequest(String from, String roomName) {
        this.from = from;
        this.roomName = roomName;
    }
    @Override public String type() { return "room_create"; }
    @Override public String from() { return from; }
    public String roomName() { return roomName; }
}
