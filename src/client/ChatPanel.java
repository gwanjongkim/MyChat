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

    private final Map<String, TypingEntry> typingEntries = new HashMap<>();


    public ChatPanel() {

        setPreferredSize(new Dimension(350, 700));
        setLayout(new BorderLayout(6,6));


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

        bottom = new JPanel(new BorderLayout(6,6));
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
        bEmoji.addActionListener(e -> openPicker());


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

    // ======================================================================
    // ★★★ 외부에서 top/bottom을 숨기기 위한 메서드 ★★★
    // ======================================================================
    public void hideTopBar() {
        if (top != null) top.setVisible(false);
    }

    public void hideBottomBar() {
        if (bottom != null) bottom.setVisible(false);
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

            bConnect.setEnabled(false);
            bDisconnect.setEnabled(true);
            bSend.setEnabled(true);
            bEmoji.setEnabled(true);
            tInput.requestFocusInWindow();

            send(new TypingMessage(from(), false, 1));

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
                }
            }

        } catch (Exception ex) {
            append("[system] connection closed\n");
        }
    }


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


    private void openPicker() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                var m = new ImageMessage(from(), bytes, guessMime(file.getName()), 128, 128);
                send(m);

            } catch (Exception ex) {
                append("[error] " + ex.getMessage() + "\n");
            }
        }
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

                Image scaled = img.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                chat.insertIcon(new ImageIcon(scaled));
                chat.replaceSelection("\n");

            } catch (Exception ignored) {}
        });
    }


    private void showTypingInline(String user) {
        SwingUtilities.invokeLater(() -> {
            try {
                TypingEntry old = typingEntries.get(user);
                if (old != null) {
                    old.ttlTimer.restart();
                    return;
                }

                var doc = chat.getStyledDocument();
                String line = "[typing] " + user + " 입력 중...\n";
                var pos = doc.createPosition(doc.getLength());
                doc.insertString(doc.getLength(), line, null);

                TypingEntry ent = new TypingEntry();
                ent.start = pos;
                ent.length = line.length();

                ent.ttlTimer = new javax.swing.Timer(TYPING_TTL_MS,
                        e -> removeTypingInline(user));

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

            if (ent.ttlTimer != null)
                ent.ttlTimer.stop();

            try {
                var doc = chat.getStyledDocument();
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

    public void autoConnect() {
        connect();
    }

}



