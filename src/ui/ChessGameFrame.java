package ui;

import client.ChatPanel;
import simplechess.main.GamePanel;

import javax.swing.*;
import java.awt.*;

public class ChessGameFrame extends JFrame {

    public ChessGameFrame(String player1Name, String player2Name) {
        super("Java Chess");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        GamePanel chessBoard = new GamePanel();
        RightPanel right = new RightPanel(player1Name, player2Name);
        ChatPanel chatPanel = right.getChatPanel();

        chessBoard.setMoveListener((fromCol, fromRow, toCol, toRow, color) ->
                chatPanel.sendMove(fromCol, fromRow, toCol, toRow, color)
        );

        chatPanel.setMoveReceiver((fromCol, fromRow, toCol, toRow, color) ->
                chessBoard.applyNetworkMove(fromCol, fromRow, toCol, toRow)
        );

        chessBoard.setTurnListener(right);

        add(chessBoard, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}






