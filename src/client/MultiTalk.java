package client;

import codec.JavaObjectCodec;
import codec.MessageCodec;
import common.ChatMessage;
import common.ImageMessage;
import common.TextMessage;
import common.TypingMessage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public final class MultiTalk extends JFrame {
    // UI
    private JTextField tInput, tUser, tHost, tPort;
    private JButton bSend, bConnect, bDisconnect, bEmoji;
    private JTextPane chat;
    private JLabel typingLabel;

    // Net
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread reader;

    // Typing-indicator state
    private final Map<String, Long> typingExpiry = new HashMap<>();
    private long lastTypingSentAt = 0L;
    private static final int TYPING_DEBOUNCE_MS = 1000; // 1초에 한 번 start 전송
    private static final int TYPING_IDLE_MS     = 1200; // 1.2초 입력 없으면 stop 전송
    private static final int TYPING_GC_MS       = 500;  // 라벨 정리 주기

    private javax.swing.Timer typingIdleTimer;
    private javax.swing.Timer typingGcTimer;

    // Codec (Strategy)
    private final MessageCodec codec = new JavaObjectCodec();

    public MultiTalk(String host, int port) {
        setTitle("Multi Talk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 520);
        buildUI(host, port);
        setVisible(true);
    }

    private void buildUI(String host, int port) {
        // top
        JPanel top = new JPanel();
        tUser = new JTextField("guest" + lastOctet(), 8);
        tHost = new JTextField(host, 12);
        tPort = new JTextField(String.valueOf(port), 5);
        bConnect = new JButton("Connect");
        bDisconnect = new JButton("Disconnect");
        bDisconnect.setEnabled(false);
        top.add(new JLabel("User:")); top.add(tUser);
        top.add(new JLabel("Host:")); top.add(tHost);
        top.add(new JLabel("Port:")); top.add(tPort);
        top.add(bConnect); top.add(bDisconnect);

        // center
        chat = new JTextPane();
        chat.setEditable(false);
        JScrollPane sc = new JScrollPane(chat);

        // bottom
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        tInput = new JTextField();
        JPanel right = new JPanel();
        bEmoji = new JButton("😊");
        bSend = new JButton("Send");
        bEmoji.setEnabled(false); bSend.setEnabled(false);
        right.add(bEmoji); right.add(bSend);

        typingLabel = new JLabel(" ");
        typingLabel.setFont(typingLabel.getFont().deriveFont(11f));
        typingLabel.setForeground(Color.GRAY);

        bottom.add(typingLabel, BorderLayout.NORTH);
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

        // typing detection (문서 변경을 한 군데서만 감지)
        tInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void onChange() {
                long now = System.currentTimeMillis();
                if (now - lastTypingSentAt >= TYPING_DEBOUNCE_MS) {
                    lastTypingSentAt = now;
                    sendTyping(true);
                }
                if (typingIdleTimer != null) typingIdleTimer.stop();
                typingIdleTimer = new javax.swing.Timer(TYPING_IDLE_MS, e -> sendTyping(false));
                typingIdleTimer.setRepeats(false);
                typingIdleTimer.start();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
        });

        // typing label GC timer
        typingGcTimer = new javax.swing.Timer(TYPING_GC_MS, e -> refreshTypingLabel());
        typingGcTimer.start();
    }

    private String lastOctet() {
        try {
            return InetAddress.getLocalHost().getHostAddress().replaceAll(".*\\.", "");
        } catch (Exception e) {
            return "000";
        }
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
        } catch (Exception ex) {
            append("[error] " + ex.getMessage() + "\n");
        }
    }

    private void disconnect() {
        try { sendTyping(false); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;

        if (typingIdleTimer != null) typingIdleTimer.stop();
        if (typingGcTimer != null) typingGcTimer.stop();
        typingExpiry.clear();
        refreshTypingLabel();

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
                    append(tm.from() + ": " + tm.text() + "\n");

                } else if (msg instanceof ImageMessage im) {
                    showImage(im);

                } else if (msg instanceof TypingMessage ty) {
                    // 내 신호는 무시
                    if (!ty.from().equals(tUser.getText().trim())) {
                        if (ty.typing()) {
                            typingExpiry.put(ty.from(), System.currentTimeMillis() + ty.ttlMs());
                        } else {
                            typingExpiry.remove(ty.from());
                        }
                        refreshTypingLabel();
                    }
                }
            }
        } catch (Exception ex) {
            append("[system] connection closed\n");
        } finally {
            SwingUtilities.invokeLater(() -> {
                bConnect.setEnabled(true); bDisconnect.setEnabled(false);
                bSend.setEnabled(false); bEmoji.setEnabled(false);
                typingExpiry.clear();
                refreshTypingLabel();
                if (typingGcTimer != null) typingGcTimer.stop();
                if (typingIdleTimer != null) typingIdleTimer.stop();
            });
        }
    }

    private void sendText() {
        String text = tInput.getText().trim();
        if (text.isEmpty()) return;
        var msg = new TextMessage(tUser.getText().trim(), text);
        try {
            send(msg);
            tInput.setText("");
            sendTyping(false); // 입력 종료 신호
        } catch (Exception ex) {
            append("[error] " + ex.getMessage() + "\n");
        }
    }

    private void openPicker() {
        EmojiPickerDialog dlg = new EmojiPickerDialog(this, file -> {
            try {
                var bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String mime = guessMime(file.getName());
                var m = new ImageMessage(tUser.getText().trim(), bytes, mime, 128, 128);
                send(m);
            } catch (Exception ex) {
                append("[error] " + ex.getMessage() + "\n");
            }
        });
        dlg.setVisible(true);
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
                chat.getDocument().insertString(chat.getDocument().getLength(), "\n", null);
                chat.setCaretPosition(chat.getDocument().getLength());
            } catch (Exception ignored) {}
        });
    }

    // ---- typing indicator helpers ----
    private void sendTyping(boolean typing) {
        if (socket == null || socket.isClosed()) return;
        try {
            var m = new TypingMessage(tUser.getText().trim(), typing, 2000); // 2초 유지 권장
            send(m);
        } catch (Exception ignored) {}
    }

    private void refreshTypingLabel() {
        long now = System.currentTimeMillis();
        typingExpiry.entrySet().removeIf(en -> en.getValue() <= now);
        if (typingExpiry.isEmpty()) {
            typingLabel.setText(" ");
        } else {
            String who = String.join(", ", typingExpiry.keySet());
            typingLabel.setText(who + " 입력 중…");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MultiTalk("localhost", 54321));
    }
}
