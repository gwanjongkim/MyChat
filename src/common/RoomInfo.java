package common;

import java.io.Serializable;

public final class RoomInfo implements Serializable {
    private final String roomId;
    private final String name;
    private final int size;
    private final boolean full;

    public RoomInfo(String roomId, String name, int size, boolean full) {
        this.roomId = roomId;
        this.name = name;
        this.size = size;
        this.full = full;
    }

    public String roomId() { return roomId; }
    public String name() { return name; }
    public int size() { return size; }
    public boolean full() { return full; }
}
