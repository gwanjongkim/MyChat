package server;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GameRoom {
    public final String roomId;
    public final String name;

    private final Set<ClientContext> members =
            Collections.synchronizedSet(new LinkedHashSet<>());

    public ClientContext white; // 첫 입장자
    public ClientContext black; // 두 번째 입장자

    // ✅ 추가: 플레이어 이름 저장
    public String whiteName;
    public String blackName;

    public GameRoom(String roomId, String name) {
        this.roomId = roomId;
        this.name = name;
    }

    public Set<ClientContext> members() { return members; }

    public int size() { return members.size(); }

    public boolean isFull() { return white != null && black != null; }
}
