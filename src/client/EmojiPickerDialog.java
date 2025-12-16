// File: src/client/EmojiPickerDialog.java
// Simple grid picker that scans ./emojis for images
// =========================
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class EmojiPickerDialog extends JDialog {
    public interface Listener { void onPick(File file); }
    private final Listener listener;

    public EmojiPickerDialog(Frame owner, Listener listener) {
        super(owner, "Pick a sticker", true);
        this.listener = listener;
        setLayout(new BorderLayout());
        var panel = new JPanel(new GridLayout(2, 3, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        File dir = new File("./res/emojis");
        List<File> images = new ArrayList<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".png") || n.endsWith(".jpg") ||
                        n.endsWith(".jpeg") || n.endsWith(".gif")) {
                    images.add(f);
                }
            }
        }
        if (images.isEmpty()) {
            panel.add(new JLabel("Put PNG/JPG/GIF files into ./emojis"));
        } else {
            for (File f : images) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img == null) continue;
                    Image thumb = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                    JButton b = new JButton(new ImageIcon(thumb));
                    b.setFocusPainted(false);
                    b.addActionListener(e -> { listener.onPick(f); dispose(); });
                    panel.add(b);
                } catch (Exception ignored) {}
            }
        }
        add(new JScrollPane(panel), BorderLayout.CENTER);
        setSize(520, 420);
        setLocationRelativeTo(owner);
    }
}
