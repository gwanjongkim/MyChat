// File: src/client/ChessChatMain.java
package client;

import javax.swing.*;
import java.awt.*;
import simplechess.main.GamePanel;

public final class ChessChatMain {


    public static void run(String playerName) {


        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("SimpleChess + Chat");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GamePanel chessBoard = new GamePanel();
            chessBoard.setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));

            ChatPanel chat = new ChatPanel();

            chat.setDefaultTarget("localhost", 54321, playerName);

            JSplitPane split = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    chessBoard,
                    chat
            );
            split.setResizeWeight(1.0);
            split.setDividerLocation(GamePanel.WIDTH);

            f.add(split, BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // 테스트용
    public static void main(String[] args) {
        run("테스트플레이어");
    }
}
