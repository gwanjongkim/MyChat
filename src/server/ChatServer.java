// File: src/server/ChatServer.java
// =========================
package server;

import codec.JavaObjectCodec;
import codec.MessageCodec;
import common.ChatMessage;
import common.TextMessage;
import common.ImageMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ChatServer {
    private final int port;
    private final MessageCodec codec = new JavaObjectCodec();     // Strategy injection point
    private final HandlerFactory factory = new HandlerFactory();  // Factory for handlers
    private final Set<ClientContext> clients = Collections.synchronizedSet(new HashSet<>());

    public ChatServer(int port) {
        this.port = port;
        // Register handlers (Factory)
        factory.register("text",  new TextMessageHandler());
        factory.register("image", new ImageMessageHandler());
        factory.register("typing", new TypingMessageHandler());
        factory.register("move", new MoveMessageHandler());

    }

    public void start() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[Server] Listening on " + port);
            while (true) {
                Socket s = ss.accept();
                System.out.println("[Server] Client connected: " + s.getRemoteSocketAddress());
                var ctx = new ClientContext(s, this, codec);
                clients.add(ctx);
                new Thread(ctx, "client-"+s.getPort()).start();
            }
        }
    }

    void dispatch(ChatMessage msg, ClientContext from) {
        var handler = factory.get(msg.type());
        if (handler != null) {
            @SuppressWarnings("unchecked")
            MessageHandler<ChatMessage> h = (MessageHandler<ChatMessage>) handler;
            h.handle(msg, from);
        }
    }

    void broadcast(ChatMessage msg, ClientContext from) {
        synchronized (clients) {
            for (var c : clients) {
                try { c.send(msg); } catch (IOException ignored) {}
            }
        }
    }

    void remove(ClientContext ctx) {
        clients.remove(ctx);
        // Optionally notify others
        try { broadcast(new TextMessage("system", "A user left."), ctx); } catch (Exception ignored) {}
    }
    // ChatServer 내부
    void broadcastExcept(common.ChatMessage msg, ClientContext except) {
        synchronized (clients) {
            for (var c : clients) {
                if (c == except) continue;
                try { c.send(msg); } catch (IOException ignored) {}
            }
        }
    }

    // Entry point
    public static void main(String[] args) throws Exception {
        new ChatServer(54321).start();
    }
}
