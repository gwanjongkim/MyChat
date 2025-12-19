package ui;

import simplechess.main.GamePanel;
import client.ChatPanel;

import javax.swing.*;
import java.awt.*;

public class ChessGameFrame extends JFrame {

    public ChessGameFrame(String whiteName, String blackName, int myColor, ChatPanel sharedChatPanel) {
        super("Java Chess");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        GamePanel chessBoard = new GamePanel(myColor);

        RightPanel right = new RightPanel(whiteName, blackName, myColor, sharedChatPanel);
        ChatPanel chatPanel = right.getChatPanel();

        chessBoard.setTurnListener(right);

        chessBoard.setMoveListener((fromCol, fromRow, toCol, toRow, color) -> {
            chatPanel.sendMove(fromCol, fromRow, toCol, toRow, color);
        });

        chatPanel.setMoveReceiver((fromCol, fromRow, toCol, toRow, color) -> {
            SwingUtilities.invokeLater(() ->
                    chessBoard.applyNetworkMove(fromCol, fromRow, toCol, toRow)
            );
        });

        add(chessBoard, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
