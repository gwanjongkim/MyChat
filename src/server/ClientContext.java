// File: src/server/ClientContext.java
// Helper representing a connected client session on server
// =========================
package server;

import codec.MessageCodec;
import common.ChatMessage;
import java.io.*;
import java.net.Socket;

public final class ClientContext implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final MessageCodec codec;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean running = true;

    public ClientContext(Socket socket, ChatServer server, MessageCodec codec) throws IOException {
        this.socket = socket;
        this.server = server;
        this.codec  = codec;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public ChatServer server() { return server; }

    @Override public void run() {
        try {
            while (running) {
                int len = in.readInt();               // length-prefixed frame
                if (len <= 0) break;
                byte[] buf = in.readNBytes(len);
                var msg = codec.decode(buf);
                server.dispatch(msg, this);           // let factory/handlers process
            }
        } catch (IOException | ClassNotFoundException ex) {
            // client disconnected or error
        } finally {
            running = false;
            server.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public synchronized void send(ChatMessage msg) throws IOException {
        byte[] data = codec.encode(msg);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }
}
