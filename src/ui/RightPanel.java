package ui;

import client.ChatPanel;
import simplechess.main.GamePanel;

import javax.swing.*;
import java.awt.*;

public class RightPanel extends JPanel implements GamePanel.TurnListener {

    JLabel lblP1;
    JLabel lblP2;
    ChatPanel chatPanel;

    String player1Name;
    String player2Name;
    int myColor;

    public RightPanel(String player1Name, String player2Name, int myColor,  ChatPanel shared) {

        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.myColor = myColor;
        this.chatPanel = shared;

        setLayout(new BorderLayout());
        setBackground(new Color(50, 54, 59));
        setPreferredSize(new Dimension(380, 900));

        JPanel info = new JPanel();
        info.setBackground(new Color(50, 54, 59));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblP1 = new JLabel(player1Name + " (White)");
        lblP2 = new JLabel(player2Name + " (Black)");
        lblP1.setForeground(Color.YELLOW);
        lblP2.setForeground(new Color(180, 180, 180));
        lblP1.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        lblP2.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        lblP1.setForeground(Color.YELLOW);
        lblP1.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        lblP1.setIconTextGap(10);

        info.add(lblP1);
        info.add(Box.createVerticalStrut(15));
        info.add(lblP2);

        chatPanel.setMinimumSize(new Dimension(350, 700));
        chatPanel.setPreferredSize(new Dimension(350, 700));

        add(info, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
    }

    @Override
    public void onTurnChanged(int color) {
        if (color == GamePanel.WHITE) {
            lblP1.setForeground(Color.YELLOW);
            lblP2.setForeground(new Color(180, 180, 180));
        } else {
            lblP1.setForeground(new Color(180, 180, 180));
            lblP2.setForeground(Color.YELLOW);
        }
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}




