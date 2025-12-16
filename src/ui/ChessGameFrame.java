package ui;

import simplechess.main.GamePanel;
import client.ChatPanel;

import javax.swing.*;
import java.awt.*;

public class ChessGameFrame extends JFrame {

    public ChessGameFrame(String player1Name, String player2Name, int myColor) {
        super("Java Chess");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        GamePanel chessBoard = new GamePanel(myColor);

        RightPanel right = new RightPanel(player1Name, player2Name, myColor);
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

        chessBoard.setGameOverListener(winnerColor -> {
            SwingUtilities.invokeLater(() ->
                    showGameOverDialog(winnerColor)
            );
        });

        chessBoard.setCheckListener(kingColor -> {
            String color = (kingColor == GamePanel.WHITE) ? "White" : "Black";
            chatPanel.sendSystemMessage(color + " 왕이 체크 입니다!");
        });

        add(chessBoard, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showGameOverDialog(int winnerColor) {
        JDialog dialog = new JDialog(this, "게임 종료", true);
        dialog.setSize(400, 250);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        String winner = (winnerColor == GamePanel.WHITE ? "White" : "Black");

        JPanel panel = new JPanel();
        panel.setBackground(new Color(30, 30, 30));
        panel.setLayout(null);

        JLabel title = new JLabel("게임 종료!", SwingConstants.CENTER);
        title.setForeground(Color.YELLOW);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        title.setBounds(50, 20, 300, 40);
        panel.add(title);

        JLabel winnerLabel = new JLabel(winner + " 승리!", SwingConstants.CENTER);
        winnerLabel.setForeground(Color.WHITE);
        winnerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        winnerLabel.setBounds(50, 70, 300, 40);
        panel.add(winnerLabel);

        JButton restartBtn = new JButton("새 게임 시작");
        restartBtn.setBounds(50, 140, 130, 40);

        JButton exitBtn = new JButton("종료");
        exitBtn.setBounds(220, 140, 130, 40);

        restartBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            new StartMenu();
        });

        exitBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
        });

        panel.add(restartBtn);
        panel.add(exitBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}







