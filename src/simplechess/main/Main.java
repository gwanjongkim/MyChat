package simplechess.main;

import javax.swing.JFrame;

public class Main {

    public static void main(String[] args) {

        JFrame window = new JFrame("Simple Chess");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Add GamePanel to the window
        GamePanel gp = new GamePanel();

        Object[] options = {"White", "Black"};
        int choice = javax.swing.JOptionPane.showOptionDialog(
                window,
                "Choose your side",
                "Color",
                javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 1) {
            gp.setMyColor(GamePanel.BLACK);  // 플레이어2(흑) → 보드 뒤집기
        } else {
            gp.setMyColor(GamePanel.WHITE);  // 기본: 백 시점
        }

        window.add(gp);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
        
        gp.launchGame();
    }
}
