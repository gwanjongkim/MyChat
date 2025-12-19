package client;

import common.*;
import codec.*;
import client.EmojiPickerDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChatPanel extends JPanel {

    // ===== 고정 포트 =====
    private static final int SERVER_PORT = 54321;

    // ===== UI =====
    public JPanel top;
    public JPanel bottom;

    private final JTextField tUser = new JTextField(8);
    private final JTextField tHost = new JTextField(12);

    private final JButton bConnect = new JButton("Connect");
    private final JButton bDisconnect = new JButton("Disconnect");

    private final JTextPane chat = new JTextPane();
    private final JTextField tInput = new JTextField();
    private final JButton bSend = new JButton("Send");
    private final JButton bEmoji = new JButton("EMOJI");

    private final JScrollPane sc = new JScrollPane(chat);

    // 채팅 영역 표시/숨김 (로비/게임 분리용)
    private boolean chatVisible = true;

    // ===== Net =====
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread reader;

    private final MessageCodec codec = new JavaObjectCodec();

    // ===== Typing =====
    private long lastTypingSentAt = 0L;
    private static final int TYPING_DEBOUNCE_MS = 800;
    private static final int TYPING_IDLE_MS = 1200;
    private static final int TYPING_TTL_MS = 2000;

    private javax.swing.Timer typingIdleTimer;

    private static final class TypingEntry {
        javax.swing.text.Position start;
        int length;
        javax.swing.Timer ttlTimer;
    }

    private final Map<String, TypingEntry> typingEntries = new HashMap<>();

    // ===== MoveMessage 전달 =====
    public interface MoveReceiver {
        void onMove(int fromCol, int fromRow, int toCol, int toRow, int color);
    }
    private MoveReceiver moveReceiver;
    public void setMoveReceiver(MoveReceiver r) { this.moveReceiver = r; }

    // ===== Room callbacks =====
    public interface RoomJoinListener { void onJoined(common.RoomJoined joined); }
    private RoomJoinListener roomJoinListener;
    public void setRoomJoinListener(RoomJoinListener l) { this.roomJoinListener = l; }

    public interface RoomListListener { void onRoomList(List<common.RoomInfo> rooms); }
    private RoomListListener roomListListener;
    public void setRoomListListener(RoomListListener l) { this.roomListListener = l; }

    // ===== IPv4 강제 선택 =====
    private static String resolveIPv4(String host) throws UnknownHostException {
        for (InetAddress a : InetAddress.getAllByName(host)) {
            if (a instanceof Inet4Address) return a.getHostAddress();
        }
        return host; // IPv6만 있는 환경이면 그대로
    }

    public ChatPanel() {
        setPreferredSize(new Dimension(350, 700));
        setLayout(new BorderLayout(6, 6));

        // ===== TOP (Connect Bar) =====
        top = new JPanel();
        top.setPreferredSize(new Dimension(10, 60));

        bDisconnect.setEnabled(false);

        top.add(new JLabel("User:"));
        top.add(tUser);
        top.add(new JLabel("Host:"));
        top.add(tHost);

        // 포트 고정 라벨만 표시
        top.add(new JLabel("Port: " + SERVER_PORT));

        top.add(bConnect);
        top.add(bDisconnect);

        // ===== CENTER (Chat view) =====
        chat.setEditable(false);

        // ===== BOTTOM (Input Bar) =====
        bottom = new JPanel(new BorderLayout(6, 6));
        JPanel right = new JPanel();
        right.add(bEmoji);
        right.add(bSend);

        bSend.setEnabled(false);
        bEmoji.setEnabled(false);

        bottom.add(tInput, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(sc, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // ✅ 버튼 리스너 (필수)
        bConnect.addActionListener(e -> autoConnect());
        bDisconnect.addActionListener(e -> disconnect());
        bSend.addActionListener(e -> sendText());
        tInput.addActionListener(e -> sendText());
        bEmoji.addActionListener(e -> emojiPicker());

        // 입력 중 상태 감지
        tInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void onChange() {
                long now = System.currentTimeMillis();

                if (now - lastTypingSentAt > TYPING_DEBOUNCE_MS) {
                    lastTypingSentAt = now;
                    sendTyping(true);
                }

                if (typingIdleTimer != null) typingIdleTimer.stop();
                typingIdleTimer = new javax.swing.Timer(TYPING_IDLE_MS, e -> sendTyping(false));
                typingIdleTimer.setRepeats(false);
                typingIdleTimer.start();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
        });
    }

    // ===== 로비/게임 화면용 표시 제어 =====
    public void setChatVisible(boolean visible) {
        this.chatVisible = visible;
        sc.setVisible(visible);
        bottom.setVisible(visible);
        revalidate();
        repaint();
    }

    /** 채팅 화면(대화 기록)을 비운다. 로비/게임 전환 시 분리하고 싶을 때 사용 */
    public void clearChat() {
        SwingUtilities.invokeLater(() -> {
            chat.setText("");
        });
    }

    public void hideTopBar() { if (top != null) top.setVisible(false); }
    public void hideBottomBar() { if (bottom != null) bottom.setVisible(false); }

    // 호환성: 예전 호출이 남아있어도 깨지지 않게
    public void setDefaultTarget(String host, int port, String user) {
        setDefaultTarget(host, user); // port는 무시 (고정 포트)
    }

    public void setDefaultTarget(String host, String user) {
        tHost.setText(host);
        tUser.setText(user);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ==============================
    //         네트워크 연결
    // ==============================
    private void connect() {
        try {
            String host = tHost.getText().trim();
            host = resolveIPv4(host);
            tHost.setText(host);

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, SERVER_PORT), 3000);

            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            append("[system] connected to " + host + ":" + SERVER_PORT + "\n");

            reader = new Thread(this::readLoop, "reader");
            reader.start();

            bConnect.setEnabled(false);
            bDisconnect.setEnabled(true);
            bSend.setEnabled(true);
            bEmoji.setEnabled(true);

            sendTyping(false);

        } catch (Exception ex) {
            append("[error] " + ex.getMessage() + "\n");
        }
    }

    private void disconnect() {
        try { sendTyping(false); } catch (Exception ignored) {}

        try { if (socket != null) socket.close(); } catch (IOException ignored) {}

        socket = null;
        in = null;
        out = null;

        if (reader != null) {
            try { reader.interrupt(); } catch (Exception ignored) {}
            reader = null;
        }

        typingEntries.clear();

        bConnect.setEnabled(true);
        bDisconnect.setEnabled(false);
        bSend.setEnabled(false);
        bEmoji.setEnabled(false);

        append("[system] disconnected\n");
    }

    public void autoConnect() {
        if (isConnected()) {
            append("[system] already connected\n");
            return;
        }
        connect();
    }

    // ==============================
    //         서버로부터 메시지 읽기
    // ==============================
    private void readLoop() {
        try {
            while (socket != null && !socket.isClosed()) {
                int len = in.readInt();
                byte[] buf = in.readNBytes(len);

                ChatMessage msg = codec.decode(buf);

                if (msg instanceof TextMessage tm) {
                    removeTypingInline(tm.from());
                    append(tm.from() + ": " + tm.text() + "\n");

                } else if (msg instanceof ImageMessage im) {
                    removeTypingInline(im.from());
                    showImage(im);

                } else if (msg instanceof TypingMessage ty) {
                    if (!ty.from().equals(from())) {
                        if (ty.typing()) showTypingInline(ty.from());
                        else removeTypingInline(ty.from());
                    }

                } else if (msg instanceof MoveMessage mm) {
                    if (moveReceiver != null) {
                        moveReceiver.onMove(mm.fromCol(), mm.fromRow(), mm.toCol(), mm.toRow(), mm.color());
                    }
                    append(formatMoveLine(mm));

                } else if (msg instanceof common.RoomJoined rj) {
                    if (roomJoinListener != null) roomJoinListener.onJoined(rj);
                    append("[system] 방 참가 완료: " + rj.roomId() + "\n");

                } else if (msg instanceof common.RoomListResponse rl) {
                    if (roomListListener != null) roomListListener.onRoomList(rl.rooms());
                }
            }

        } catch (Exception ex) {
            append("[system] connection closed\n");
            disconnect();
        }
    }

    // ==============================
    //       MoveMessage 전송
    // ==============================
    public void sendMove(int fromCol, int fromRow, int toCol, int toRow, int color) {
        try {
            MoveMessage m = new MoveMessage(from(), fromCol, fromRow, toCol, toRow, color);
            send(m);
        } catch (Exception ignored) {}
    }

    // ==============================
    //       Room 요청 API
    // ==============================
    public void requestRoomList() {
        try { send(new common.RoomListRequest(from())); }
        catch (Exception e) { append("[error] " + e.getMessage() + "\n"); }
    }

    public void requestCreateRoom(String roomName) {
        try { send(new common.RoomCreateRequest(from(), roomName)); }
        catch (Exception e) { append("[error] " + e.getMessage() + "\n"); }
    }

    public void requestJoinRoom(String roomId) {
        try { send(new common.RoomJoinRequest(from(), roomId)); }
        catch (Exception e) { append("[error] " + e.getMessage() + "\n"); }
    }

    // ==============================
    // 텍스트 전송
    // ==============================
    private void sendText() {
        String text = tInput.getText().trim();
        if (text.isEmpty()) return;

        var msg = new TextMessage(from(), text);

        try {
            send(msg);
            tInput.setText("");
            sendTyping(false);

        } catch (Exception ex) {
            append("[error] " + ex.getMessage() + "\n");
        }
    }

    // ==============================
    // 이미지(이모지) 전송
    // ==============================
    private void emojiPicker() {
        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame) ? (Frame) w : null;

        EmojiPickerDialog dialog = new EmojiPickerDialog(owner, file -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                var m = new ImageMessage(from(), bytes, guessMime(file.getName()), 128, 128);
                send(m);
            } catch (Exception ex) {
                append("[error] " + ex.getMessage() + "\n");
            }
        });

        dialog.setVisible(true);
    }

    private String guessMime(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private void send(ChatMessage msg) throws Exception {
        if (out == null) throw new IllegalStateException("Not connected");
        byte[] data = codec.encode(msg);
        synchronized (this) {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }
    }

    private String from() {
        String u = tUser.getText().trim();
        return u.isEmpty() ? "user" : u;
    }

    // ==============================
    // UI 출력
    // ==============================
    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            try {
                chat.getDocument().insertString(chat.getDocument().getLength(), s, null);
                chat.setCaretPosition(chat.getDocument().getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void showImage(ImageMessage im) {
        SwingUtilities.invokeLater(() -> {
            try {
                var img = ImageIO.read(new ByteArrayInputStream(im.data()));
                if (img == null) return;

                StyledDocument doc = chat.getStyledDocument();
                chat.setCaretPosition(doc.getLength());

                doc.insertString(doc.getLength(), im.from() + ": ", null);

                chat.setCaretPosition(doc.getLength());
                Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                chat.insertIcon(new ImageIcon(scaled));

                chat.setCaretPosition(doc.getLength());
                doc.insertString(doc.getLength(), "\n", null);

            } catch (Exception ignored) {}
        });
    }

    // 체스 좌표를 채팅 문자열로 변환해서 한 줄 만들어주는 헬퍼
    private String formatMoveLine(MoveMessage mm) {
        char fromFile = (char) ('a' + mm.fromCol());
        int  fromRank = 8 - mm.fromRow();

        char toFile   = (char) ('a' + mm.toCol());
        int  toRank   = 8 - mm.toRow();

        return String.format("%s: %c%d -> %c%d\n",
                mm.from(), fromFile, fromRank, toFile, toRank);
    }

    // ==============================
    // 타이핑 표시
    // ==============================
    private void showTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            try {
                TypingEntry old = typingEntries.get(user);
                if (old != null) {
                    old.ttlTimer.restart();
                    return;
                }

                StyledDocument doc = chat.getStyledDocument();
                String line = "[typing] " + user + " 입력 중...\n";
                int start = doc.getLength();
                doc.insertString(doc.getLength(), line, null);

                TypingEntry ent = new TypingEntry();
                ent.start = doc.createPosition(start);
                ent.length = line.length();

                ent.ttlTimer = new javax.swing.Timer(TYPING_TTL_MS, e -> removeTypingInline(user));
                ent.ttlTimer.setRepeats(false);
                ent.ttlTimer.start();

                typingEntries.put(user, ent);
                chat.setCaretPosition(doc.getLength());

            } catch (Exception ignored) {}
        });
    }

    private void removeTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            TypingEntry ent = typingEntries.remove(user);
            if (ent == null) return;

            if (ent.ttlTimer != null) ent.ttlTimer.stop();

            try {
                StyledDocument doc = chat.getStyledDocument();
                int off = ent.start.getOffset();
                int len = Math.min(ent.length, doc.getLength() - off);
                if (len > 0) doc.remove(off, len);
            } catch (Exception ignored) {}
        });
    }

    private void sendTyping(boolean typing) {
        if (!isConnected()) return;
        var m = new TypingMessage(from(), typing, 2000);
        try { send(m); } catch (Exception ignored) {}
    }

    public void appendSystemMessage(String msg) {
        append("[system] " + msg + "\n");
    }
}
