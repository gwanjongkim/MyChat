// File: src/client/ChatPanel.java
package client;

import common.*;
import codec.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public final class ChatPanel extends JPanel {
    // UI
    private JTextField tUser = new JTextField(8);
    private JTextField tHost = new JTextField(12);
    private JTextField tPort = new JTextField(5);
    private JButton bConnect = new JButton("Connect");
    private JButton bDisconnect = new JButton("Disconnect");
    private JTextPane chat = new JTextPane();
    private JTextField tInput = new JTextField();
    private JButton bSend = new JButton("Send");
    private JButton bEmoji = new JButton("EMOJI");

    // Net
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread reader;
    private final MessageCodec codec = new JavaObjectCodec();

    // Typing (입력중) 텍스트로 표시

    private long lastTypingSentAt = 0L;
    private static final int TYPING_DEBOUNCE_MS = 800;
    private static final int TYPING_IDLE_MS = 1200;
    private static final int TYPING_TTL_MS = 2000; // 2초 후 자동 삭제
    private javax.swing.Timer typingIdleTimer;

    // 입력중 라인을 문서에서 지우기 위해 위치/길이를 저장
    private static final class TypingEntry {
        javax.swing.text.Position start; // 라인 시작 위치(문서가 커져도 위치 추적됨)
        int length;                      // 라인 길이
        javax.swing.Timer ttlTimer;      // TTL 지나면 삭제
    }
    private final java.util.Map<String, TypingEntry> typingEntries = new java.util.HashMap<>();

    public ChatPanel() {
        setLayout(new BorderLayout(6,6));

        // top bar
        JPanel top = new JPanel();
        bDisconnect.setEnabled(false);
        top.add(new JLabel("User:")); top.add(tUser);
        top.add(new JLabel("Host:")); top.add(tHost);
        top.add(new JLabel("Port:")); top.add(tPort);
        top.add(bConnect); top.add(bDisconnect);

        // center chat view
        chat.setEditable(false);
        JScrollPane sc = new JScrollPane(chat);

        // bottom input
        JPanel bottom = new JPanel(new BorderLayout(6,6));
        JPanel right = new JPanel();
        right.add(bEmoji); right.add(bSend);
        bSend.setEnabled(false); bEmoji.setEnabled(false);
        bottom.add(tInput, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(sc, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // actions
        bConnect.addActionListener(e -> connect());
        bDisconnect.addActionListener(e -> disconnect());
        bSend.addActionListener(e -> sendText());
        tInput.addActionListener(e -> sendText());
        bEmoji.addActionListener(e -> openPicker());

        // typing wire
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

        //typingGcTimer = new javax.swing.Timer(300, e -> gcTyping());
        //typingGcTimer.start();
    }

    public void setDefaultTarget(String host, int port, String user) {
        tHost.setText(host);
        tPort.setText(String.valueOf(port));
        tUser.setText(user);
    }

    private void connect() {
        try {
            String host = tHost.getText().trim();
            int port = Integer.parseInt(tPort.getText().trim());
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            append("[system] connected to " + host + ":" + port + "\n");

            reader = new Thread(this::readLoop, "reader");
            reader.start();

            bConnect.setEnabled(false); bDisconnect.setEnabled(true);
            bSend.setEnabled(true); bEmoji.setEnabled(true);
            tInput.requestFocusInWindow();

            // 접속 직후 내 uid를 서버에 알리고 싶다면(옵션):
            send(new TypingMessage(from(), false, 1)); // 존재 확인용 가벼운 신호
        } catch (Exception ex) {
            append("[error] " + ex.getMessage() + "\n");
        }
    }

    private void disconnect() {
        try { sendTyping(false); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        typingEntries.clear();
        bConnect.setEnabled(true); bDisconnect.setEnabled(false);
        bSend.setEnabled(false); bEmoji.setEnabled(false);
        append("[system] disconnected\n");
    }

    private void readLoop() {
        try {
            while (socket != null && !socket.isClosed()) {
                int len = in.readInt();
                byte[] buf = in.readNBytes(len);
                ChatMessage msg = codec.decode(buf);

                if (msg instanceof TextMessage tm) {
                    removeTypingInline(tm.from());           // 실제 메시지 도착 -> typing 줄 즉시 삭제
                    append(tm.from() + ": " + tm.text() + "\n");
                } else if (msg instanceof ImageMessage im) {
                    removeTypingInline(im.from());           // 이미지도 도착하면 삭제(선택)
                    showImage(im);
                } else if (msg instanceof TypingMessage ty) {
                    if (!ty.from().equals(from())) {
                        if (ty.typing()) showTypingInline(ty.from());
                        else             removeTypingInline(ty.from());
                    }
                }
            }
        } catch (Exception ex) {
            append("[system] connection closed\n");
        } finally {
            SwingUtilities.invokeLater(() -> {
                bConnect.setEnabled(true); bDisconnect.setEnabled(false);
                bSend.setEnabled(false); bEmoji.setEnabled(false);
            });
        }
    }

    private void sendText() {
        String text = tInput.getText().trim();
        if (text.isEmpty()) return;
        var msg = new TextMessage(from(), text);
        try { send(msg); tInput.setText(""); sendTyping(false); }//typing 종료 신호
        catch (Exception ex) { append("[error] "+ex.getMessage()+"\n"); }
    }

    private void openPicker() {
        // 간단 버전: 파일 다이얼로그로 이미지 선택
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                var m = new ImageMessage(from(), bytes, guessMime(file.getName()), 128, 128);
                send(m);
            } catch (Exception ex) { append("[error] "+ex.getMessage()+"\n"); }
        }
    }

    private String guessMime(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
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

    private String from() { return tUser.getText().trim(); }

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
                int w = (im.widthHint() > 0 ? im.widthHint() : 128);
                int h = (im.heightHint() > 0 ? im.heightHint() : 128);
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                chat.insertIcon(new ImageIcon(scaled));
                chat.replaceSelection("\n");
            } catch (Exception ignored) {}
        });
    }

    // ---- typing 처리: 채팅창에 일반 텍스트로 표시 ----
    // [typing] user 입력 중... 을 채팅창 끝에 1줄만 표기(이미 있으면 TTL 연장)
    private void showTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            try {
                TypingEntry old = typingEntries.get(user);
                if (old != null) {              // 이미 표시 중이면 TTL만 연장
                    old.ttlTimer.restart();
                    return;
                }
                javax.swing.text.StyledDocument doc = chat.getStyledDocument();
                String line = "[typing] " + user + " 입력 중...\n";
                javax.swing.text.Position pos = doc.createPosition(doc.getLength());
                doc.insertString(doc.getLength(), line, null);

                TypingEntry ent = new TypingEntry();
                ent.start = pos;
                ent.length = line.length();
                ent.ttlTimer = new javax.swing.Timer(TYPING_TTL_MS, e -> removeTypingInline(user));
                ent.ttlTimer.setRepeats(false);
                ent.ttlTimer.start();

                typingEntries.put(user, ent);
                chat.setCaretPosition(doc.getLength());
            } catch (javax.swing.text.BadLocationException ignored) {}
        });
    }

    // 문서에서 해당 유저의 [typing] 라인을 제거
    private void removeTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            TypingEntry ent = typingEntries.remove(user);
            if (ent == null) return;
            if (ent.ttlTimer != null) ent.ttlTimer.stop();
            try {
                javax.swing.text.StyledDocument doc = chat.getStyledDocument();
                int off = ent.start.getOffset();
                int len = Math.min(ent.length, Math.max(0, doc.getLength() - off));
                if (len > 0) doc.remove(off, len);
            } catch (javax.swing.text.BadLocationException ignored) {}
        });
    }

    private void sendTyping(boolean typing) {
        if (socket == null || socket.isClosed()) return;
        var m = new TypingMessage(from(), typing, 2000);
        try { send(m); } catch (Exception ignored) {}
    }
}
