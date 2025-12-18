package server;

import codec.JavaObjectCodec;
import codec.MessageCodec;
import common.ChatMessage;
import common.TextMessage;
import ui.ServerLogFrame;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ChatServer {

    private final int port;
    private final MessageCodec codec = new JavaObjectCodec();
    private final HandlerFactory factory = new HandlerFactory();
    private final Set<ClientContext> clients =
            Collections.synchronizedSet(new HashSet<>());
    private final ServerLogFrame logFrame;

    public ChatServer(int port) {
        this.port = port;
        this.logFrame = new ServerLogFrame();

        factory.register("text", new TextMessageHandler());
        factory.register("image", new ImageMessageHandler());
        factory.register("typing", new TypingMessageHandler());

        logFrame.log("서버 초기화 완료 (포트: " + port + ")");
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(port);
        logFrame.log("서버 실행 중... 클라이언트 접속 대기");

        while (true) {
            Socket s = ss.accept();
            logFrame.log("클라이언트 접속: " + s.getRemoteSocketAddress());

            ClientContext ctx = new ClientContext(s, this, codec);
            clients.add(ctx);

            new Thread(ctx, "client-" + s.getPort()).start();
        }
    }

    void dispatch(ChatMessage msg, ClientContext from) {

        if (!"typing".equals(msg.type())) {
            logFrame.log(
                    "수신 ▶ 보낸이: " + msg.from() +
                            " | 메시지 타입: " + msg.type()
            );
        }

        var handler = factory.get(msg.type());
        if (handler != null) {
            @SuppressWarnings("unchecked")
            MessageHandler<ChatMessage> h =
                    (MessageHandler<ChatMessage>) handler;
            h.handle(msg, from);
        } else {
            logFrame.log("처리 불가 메시지 타입: " + msg.type());
        }
    }

    void broadcast(ChatMessage msg, ClientContext from) {

        if (!"typing".equals(msg.type())) {
            logFrame.log(
                    "전체 전송 ▶ 보낸이: " + msg.from() +
                            " | 메시지 타입: " + msg.type() +
                            " | 대상: 모든 클라이언트"
            );
        }

        synchronized (clients) {
            for (var c : clients) {
                try {
                    c.send(msg);
                } catch (IOException e) {
                    logFrame.log("전송 실패: " + e.getMessage());
                }
            }
        }
    }

    void broadcastExcept(ChatMessage msg, ClientContext except) {

        if (!"typing".equals(msg.type())) {
            logFrame.log(
                    "중계 전송 ▶ 보낸이: " + msg.from() +
                            " | 메시지 타입: " + msg.type() +
                            " | 대상: 본인 제외 모든 클라이언트"
            );
        }

        synchronized (clients) {
            for (var c : clients) {
                if (c == except) continue;
                try {
                    c.send(msg);
                } catch (IOException e) {
                    logFrame.log("전송 실패: " + e.getMessage());
                }
            }
        }
    }

    void remove(ClientContext ctx) {
        clients.remove(ctx);
        logFrame.log("클라이언트 연결 종료");

        try {
            broadcast(new TextMessage("system", "사용자 1명이 퇴장했습니다."), ctx);
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        new ChatServer(54321).start();
    }
}

