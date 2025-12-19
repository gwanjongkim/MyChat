package server;

import common.RoomListRequest;
import common.RoomListResponse;

public final class RoomListHandler implements MessageHandler<RoomListRequest> {

    @Override
    public void handle(RoomListRequest msg, ClientContext ctx) {
        try {
            ctx.send(new RoomListResponse(ctx.server().listRooms()));
        } catch (Exception ignored) {}
    }
}
