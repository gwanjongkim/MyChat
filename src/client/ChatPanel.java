package client;

import common.*;
import codec.*;
import client.EmojiPickerDialog;


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public final class ChatPanel extends JPanel {

    public JPanel top;
    public JPanel bottom;

    private JTextField tUser = new JTextField(8);
    private JTextField tHost = new JTextField(12);
    private JTextField tPort = new JTextField(5);

    private JButton bConnect = new JButton("Connect");
    private JButton bDisconnect = new JButton("Disconnect");

    private JTextPane chat = new JTextPane();
    private JTextField tInput = new JTextField();
    private JButton bSend = new JButton("Send");
    private JButton bEmoji = new JButton("EMOJI");

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread reader;

    private final MessageCodec codec = new JavaObjectCodec();

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
    // ChatPanel.java 내부에 유틸 추가
    private static String resolveIPv4(String host) throws UnknownHostException {
        // IPv4만 강제로 선택
        for (InetAddress a : InetAddress.getAllByName(host)) {
            if (a instanceof Inet4Address) return a.getHostAddress();
        }
        // 못 찾으면 원본 그대로 (혹시 IPv6만 있는 환경)
        return host;
    }

    private final Map<String, TypingEntry> typingEntries = new HashMap<>();

    // ==============================
    //        MoveMessage 전달
    // ==============================
    public interface MoveReceiver {
        void onMove(int fromCol, int fromRow, int toCol, int toRow, int color);
    }

    private MoveReceiver moveReceiver;

    public void setMoveReceiver(MoveReceiver r) {
        this.moveReceiver = r;
    }

    public ChatPanel() {

        setPreferredSize(new Dimension(350, 700));
        setLayout(new BorderLayout(6, 6));

        top = new JPanel();
        top.setPreferredSize(new Dimension(10, 60));
        bDisconnect.setEnabled(false);

        top.add(new JLabel("User:"));
        top.add(tUser);
        top.add(new JLabel("Host:"));
        top.add(tHost);
        top.add(new JLabel("Port:"));
        top.add(tPort);
        top.add(bConnect);
        top.add(bDisconnect);

        chat.setEditable(false);
        JScrollPane sc = new JScrollPane(chat);

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

        bConnect.addActionListener(e -> connect());
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
                typingIdleTimer = new javax.swing.Timer(TYPING_IDLE_MS,
                        e -> sendTyping(false));
                typingIdleTimer.setRepeats(false);
                typingIdleTimer.start();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e){ onChange(); }
        });
    }

    public void hideTopBar() { if (top != null) top.setVisible(false); }
    public void hideBottomBar() { if (bottom != null) bottom.setVisible(false); }

    public void setDefaultTarget(String host, int port, String user) {
        tHost.setText(host);
        tPort.setText(String.valueOf(port));
        tUser.setText(user);
    }

    // ==============================
    //         네트워크 연결
    // ==============================
    private void connect() {
        try {
            String host = tHost.getText().trim();
            host = resolveIPv4(host);   // ← 여기서 IPv4로 치환
            tHost.setText(host);        // UI에도 IPv4 문자열 저장
            int port = Integer.parseInt(tPort.getText().trim());

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);

            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            append("[system] connected to " + host + ":" + port + "\n");

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
        typingEntries.clear();

        bConnect.setEnabled(true);
        bDisconnect.setEnabled(false);
        bSend.setEnabled(false);
        bEmoji.setEnabled(false);

        append("[system] disconnected\n");
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
                        moveReceiver.onMove(
                                mm.fromCol(),
                                mm.fromRow(),
                                mm.toCol(),
                                mm.toRow(),
                                mm.color()
                        );
                    } {

                        // 2) 채팅창에 "플레이어: e2 -> e4" 한 줄 남기기
                        append(formatMoveLine(mm));
                    }
                }
            }

        } catch (Exception ex) {
            append("[system] connection closed\n");
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
    // 텍스트
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
    // 이미지 전송
    private void emojiPicker() {
        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame) ? (Frame) w : null;
        EmojiPickerDialog dialog = new EmojiPickerDialog(owner, file -> {
            try {
                // res/emojis 안의 선택된 이미지 파일을 그대로 읽어서 전송
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                var m = new ImageMessage(
                        from(),
                        bytes,
                        guessMime(file.getName()),
                        128,
                        128
                );
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
        byte[] data = codec.encode(msg);
        synchronized (this) {
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }
    }

    private String from() {
        return tUser.getText().trim();
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

                // 항상 문서 맨 끝으로 커서 이동
                javax.swing.text.StyledDocument doc = chat.getStyledDocument();
                chat.setCaretPosition(doc.getLength());

                // 1) "보낸사람: " 텍스트 먼저 출력
                doc.insertString(doc.getLength(), im.from() + ": ", null);

                // 2) 그 뒤에 이모지 아이콘 삽입
                chat.setCaretPosition(doc.getLength());   // 다시 끝으로
                Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                chat.insertIcon(new ImageIcon(scaled));

                // 3) 마지막에 줄바꿈 문자 추가 (replaceSelection 말고 insertString)
                chat.setCaretPosition(doc.getLength());   // 아이콘 뒤로 이동
                doc.insertString(doc.getLength(), "\n", null);

            } catch (Exception ignored) {}
        });
    }

    public void sendSystemMessage(String text) {
        try {
            send(new TextMessage("system", text));
        } catch (Exception ignored) {}
    }

    // 체스 좌표를 채팅 문자열로 변환해서 한 줄 만들어주는 헬퍼
    private String formatMoveLine(MoveMessage mm) {
        // col,row 가 0~7 이라고 가정하고 체스 표기 (a1 ~ h8)로 변환
        char fromFile = (char) ('a' + mm.fromCol());    // 0→'a', 1→'b' ...
        int  fromRank = 8 - mm.fromRow();              // 0→8, 7→1

        char toFile   = (char) ('a' + mm.toCol());
        int  toRank   = 8 - mm.toRow();

        // 예: "플레이어1: e2 -> e4"
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
                javax.swing.text.StyledDocument doc = chat.getStyledDocument();
                //var doc = chat.getStyledDocument();
                String line = "[typing] " + user + " 입력 중...\n";
                //var pos = doc.createPosition(doc.getLength());
                int start = doc.getLength();
                doc.insertString(doc.getLength(), line, null);

                TypingEntry ent = new TypingEntry();
                ent.start = doc.createPosition(start);;
                ent.length = line.length();

                ent.ttlTimer = new javax.swing.Timer(TYPING_TTL_MS,
                        e -> removeTypingInline(user));
                ent.ttlTimer.setRepeats(false);
                ent.ttlTimer.start();

                typingEntries.put(user, ent);
                chat.setCaretPosition(doc.getLength());

            } catch (javax.swing.text.BadLocationException ignored) {}
        });
    }

    private void removeTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            TypingEntry ent = typingEntries.remove(user);
            if (ent == null) return;

            if (ent.ttlTimer != null)
                ent.ttlTimer.stop();

            try {
                //var doc = chat.getStyledDocument();
                javax.swing.text.StyledDocument doc = chat.getStyledDocument();
                int off = ent.start.getOffset();
                int len = Math.min(ent.length, doc.getLength() - off);

                if (len > 0) doc.remove(off, len);

            } catch (Exception ignored) {}
        });
    }

    private void sendTyping(boolean typing) {
        if (socket == null || socket.isClosed()) return;

        var m = new TypingMessage(from(), typing, 2000);
        try { send(m); } catch (Exception ignored) {}
    }

    public void appendSystemMessage(String msg) {
        append("[system] " + msg + "\n");
    }

    public void autoConnect() {
        connect();
    }
}

