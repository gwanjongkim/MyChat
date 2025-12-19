package server;

import common.RoomJoinRequest;
import common.RoomJoined;
import common.TextMessage;
import simplechess.main.GamePanel;

public final class RoomJoinHandler implements MessageHandler<RoomJoinRequest> {

    @Override
    public void handle(RoomJoinRequest msg, ClientContext joiner) {
        try {
            String roomId = msg.roomId();
            if (roomId == null || roomId.trim().isEmpty()) {
                joiner.send(new TextMessage("system", "roomId가 비어있습니다."));
                return;
            }

            GameRoom r = joiner.server().joinRoom(roomId.trim(), joiner);
            if (r == null) {
                joiner.send(new TextMessage("system", "방 참가 실패 (없는 방이거나 이미 가득 참)"));
                return;
            }

            // ✅ 참가자 이름 저장
            r.blackName = msg.from();
            if (r.whiteName == null) r.whiteName = "White";

            // ✅ 참가자(Black)에게 참가 완료
            joiner.send(new RoomJoined(r.roomId, GamePanel.BLACK, r.whiteName, r.blackName));

            // ✅ 기존 White에게도 “상대가 들어옴” 통지 (중요!)
            if (r.white != null) {
                r.white.send(new RoomJoined(r.roomId, GamePanel.WHITE, r.whiteName, r.blackName));
            }

        } catch (Exception e) {
            try { joiner.send(new TextMessage("system", "방 참가 중 오류: " + e.getMessage())); }
            catch (Exception ignored) {}
        }
    }
}
