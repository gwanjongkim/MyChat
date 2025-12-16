package ui;


import javax.swing.*;
import java.awt.*;

public class StartMenu extends JFrame {

    public StartMenu() {
        setTitle("Java Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setPreferredSize(new Dimension(1000, 800));
        pack();
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(30, 39, 56));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(150, 200, 150, 200));

        JLabel title = new JLabel("Java Chess");
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(new Color(255, 204, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn1 = createMenuButton("플레이어1 시작", "/ui/icons/user.png");
        JButton btn2 = createMenuButton("플레이어2 시작", "/ui/icons/user.png");
        JButton btnExit = createMenuButton("종료", "/ui/icons/exit.png");

        // ★ 플레이어1이 White, 플레이어2가 Black
        btn1.addActionListener(e -> {
            new ChessGameFrame("플레이어1", "플레이어2", simplechess.main.GamePanel.WHITE);
            dispose();
        });

        // ★ 이 버튼을 누르면 플레이어2가 White, 플레이어1이 Black
        btn2.addActionListener(e -> {
            new ChessGameFrame("플레이어2", "플레이어1", simplechess.main.GamePanel.BLACK);
            dispose();
        });

        btnExit.addActionListener(e -> System.exit(0));

        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(80));
        mainPanel.add(btn1);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(btn2);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(btnExit);

        add(mainPanel);
        setVisible(true);
    }

    private JButton createMenuButton(String text, String iconPath) {
        JButton btn = new JButton(text);

        btn.setFocusable(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(55, 65, 81));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(250, 60));
        btn.setMaximumSize(new Dimension(350, 80));

        btn.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(70, 80, 95), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                )
        );

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image img = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            System.out.println("아이콘 로딩 실패: " + iconPath);
        }

        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StartMenu::new);
    }
}







