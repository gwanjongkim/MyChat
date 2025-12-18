package ui;

import javax.swing.*;
import java.awt.*;

public class ServerLogFrame extends JFrame {

    private JTextArea logArea;

    public ServerLogFrame() {
        setTitle("서버 메시지 로그");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);

        logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}


