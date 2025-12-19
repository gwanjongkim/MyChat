package common;

import java.util.List;

public final class RoomListResponse implements ChatMessage {
    private final String from = "system";
    private final List<RoomInfo> rooms;

    public RoomListResponse(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }

    @Override public String type() { return "room_list"; }
    @Override public String from() { return from; }

    public List<RoomInfo> rooms() { return rooms; }
}
