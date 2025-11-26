// File: src/client/ChessChatMain.java
package client;

import javax.swing.*;
import java.awt.*;

import simplechess.main.GamePanel;
public final class ChessChatMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("SimpleChess + Chat");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1000, 700);

            // === 1) 체스 보드 쪽 (너의 실제 보드/프레임으로 교체) ===
            // 예) GamePanel 이 JPanel을 상속한다고 가정


            JComponent  chessBoard = new GamePanel();
            // === 2) 채팅 패널 ===
            ChatPanel chat = new ChatPanel(); // 아래 제공
            // 서버 기본 값
            chat.setDefaultTarget("localhost", 54321, "guest" + (int)(Math.random()*900+100));
            // === 3) 레이아웃 ===
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chessBoard, chat);
            split.setDividerLocation(650);
            f.add(split, BorderLayout.CENTER);
            //chessBoard.setLayout(new BorderLayout());
            //chessBoard.add(new JLabel("<< 여기 GamePanel 교체 >>", SwingConstants.CENTER), BorderLayout.CENTER);

            f.setVisible(true);
        });
    }
}
