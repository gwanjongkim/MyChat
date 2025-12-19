package common;

public final class RoomListRequest implements ChatMessage {
    private final String from;

    public RoomListRequest(String from) {
        this.from = from;
    }

    @Override public String type() { return "room_list_req"; }
    @Override public String from() { return from; }
}
