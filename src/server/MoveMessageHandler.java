package server;

import common.MoveMessage;

public class MoveMessageHandler implements MessageHandler<MoveMessage> {

    @Override
    public void handle(MoveMessage msg, ClientContext ctx) {

        // 서버가 MoveMessage를 모든 클라이언트에게 전송
        ctx.server().broadcast(msg, null);
        // null = 보낸 사람 포함 전체에게 브로드캐스트
    }
}

