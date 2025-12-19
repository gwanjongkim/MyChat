package server;

import common.RoomCreateRequest;
import common.RoomJoined;
import common.TextMessage;
import simplechess.main.GamePanel;

public final class RoomCreateHandler implements MessageHandler<RoomCreateRequest> {

    @Override
    public void handle(RoomCreateRequest msg, ClientContext ctx) {
        try {
            String roomName = msg.roomName();
            if (roomName == null || roomName.trim().isEmpty()) {
                ctx.send(new TextMessage("system", "방 이름이 비어있습니다."));
                return;
            }

            GameRoom r = ctx.server().createRoom(roomName.trim(), ctx);
            if (r == null) {
                ctx.send(new TextMessage("system", "방 생성 실패"));
                return;
            }

            // ✅ 플레이어 이름 저장
            r.whiteName = msg.from();
            r.blackName = "(대기중)";

            // ✅ 생성자는 WHITE로 참가 완료 통지
            ctx.send(new RoomJoined(r.roomId, GamePanel.WHITE, r.whiteName, r.blackName));

        } catch (Exception e) {
            try { ctx.send(new TextMessage("system", "방 생성 중 오류: " + e.getMessage())); }
            catch (Exception ignored) {}
        }
    }
}
