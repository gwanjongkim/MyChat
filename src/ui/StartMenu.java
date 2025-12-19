package ui;

import client.ChatPanel;
import common.RoomInfo;
import common.RoomJoined;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StartMenu extends JFrame {

    private static final Color BG = new Color(30, 39, 56);
    private static final Color CARD = new Color(50, 54, 59);
    private static final Color CARD2 = new Color(60, 64, 70);
    private static final Color FG = new Color(230, 230, 230);

    private final ChatPanel chatPanel = new ChatPanel();

    private final DefaultListModel<RoomInfo> roomModel = new DefaultListModel<>();
    private final JList<RoomInfo> roomList = new JList<>(roomModel);

    // ✅ 버튼: 예전 스타일 적용
    private final JButton btnRefresh = createMenuButton("방 목록 새로고침", "/ui/icons/user.png");
    private final JButton btnCreate  = createMenuButton("게임 생성", "/ui/icons/user.png");
    private final JButton btnJoin    = createMenuButton("게임 참가", "/ui/icons/user.png");
    private final JButton btnExit    = createMenuButton("종료", "/ui/icons/exit.png");

    public StartMenu() {
        setTitle("Java Chess - Lobby");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1100, 800));
        pack();
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.setBackground(BG);
        setContentPane(root);

        // ===== 상단 타이틀 바 =====
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);

        JLabel title = new JLabel("Java Chess Lobby");
        title.setForeground(new Color(255, 204, 0));
        title.setFont(new Font("Arial", Font.BOLD, 36));

        JLabel tip = new JLabel("① 서버 Connect → ② 방 새로고침/생성/참가");
        tip.setForeground(new Color(190, 190, 190));
        tip.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        top.add(title, BorderLayout.WEST);
        top.add(tip, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // ===== 가운데: 왼쪽(방 목록) + 오른쪽(연결/채팅) =====
        JPanel center = new JPanel(new GridLayout(1, 2, 14, 14));
        center.setBackground(BG);
        root.add(center, BorderLayout.CENTER);

        // ---------- 방 목록 카드 ----------
        JPanel roomCard = new JPanel(new BorderLayout(10, 10));
        roomCard.setBackground(CARD);
        roomCard.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel roomTitle = new JLabel("게임 방 목록");
        roomTitle.setForeground(FG);
        roomTitle.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        roomCard.add(roomTitle, BorderLayout.NORTH);

        roomList.setBackground(CARD2);
        roomList.setForeground(FG);
        roomList.setSelectionBackground(new Color(80, 84, 92));
        roomList.setSelectionForeground(FG);
        roomList.setFixedCellHeight(34);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (value instanceof RoomInfo r) {
                    String status = r.full() ? " (가득참)" : "";
                    setText(String.format("[%s] (%d/2)%s  %s", r.roomId(), r.size(), status, r.name()));
                }
                setBackground(isSelected ? new Color(80, 84, 92) : CARD2);
                setForeground(FG);
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(roomList);
        sp.setBorder(BorderFactory.createLineBorder(new Color(70, 80, 95), 1));
        roomCard.add(sp, BorderLayout.CENTER);

        // 버튼 영역(예전 스타일 버튼이 크니까 GridLayout 간격 여유)
        JPanel buttons = new JPanel(new GridLayout(0, 1, 14, 14));
        buttons.setBackground(CARD);
        buttons.add(btnRefresh);
        buttons.add(btnCreate);
        buttons.add(btnJoin);
        buttons.add(btnExit);
        roomCard.add(buttons, BorderLayout.SOUTH);

        // ---------- 연결/채팅 카드 ----------
        JPanel connectCard = new JPanel(new BorderLayout(10, 10));
        connectCard.setBackground(CARD);
        connectCard.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel connectTitle = new JLabel("서버 연결 / 채팅");
        connectTitle.setForeground(FG);
        connectTitle.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        connectCard.add(connectTitle, BorderLayout.NORTH);


        connectCard.add(chatPanel, BorderLayout.CENTER);

        center.add(roomCard);
        center.add(connectCard);

        // ===== 이벤트 =====
        btnRefresh.addActionListener(e -> chatPanel.requestRoomList());

        btnCreate.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "방 이름을 입력하세요", "방 생성", JOptionPane.QUESTION_MESSAGE);
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) return;
            chatPanel.requestCreateRoom(name);
        });

        btnJoin.addActionListener(e -> joinSelectedRoom());
        btnExit.addActionListener(e -> System.exit(0));

        roomList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) joinSelectedRoom();
            }
        });

        // ===== ChatPanel 콜백 =====
        chatPanel.setRoomListListener(rooms -> SwingUtilities.invokeLater(() -> {
            roomModel.clear();
            for (RoomInfo r : rooms) roomModel.addElement(r);
        }));

        chatPanel.setRoomJoinListener(joined -> SwingUtilities.invokeLater(() -> enterGame(joined)));

        setVisible(true);
    }

    private void joinSelectedRoom() {
        RoomInfo sel = roomList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "참가할 방을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (sel.full()) {
            JOptionPane.showMessageDialog(this, "이미 가득 찬 방입니다.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }
        chatPanel.requestJoinRoom(sel.roomId());
    }

    private void enterGame(RoomJoined joined) {
        chatPanel.setRoomJoinListener(null);
        chatPanel.setRoomListListener(null);

        // ✅ ChatPanel을 다른 프레임으로 옮기기 전에 부모에서 떼기 (필수)
        Container p = chatPanel.getParent();
        if (p != null) {
            p.remove(chatPanel);
            p.revalidate();
            p.repaint();
        }

        new ChessGameFrame(joined.whiteName(), joined.blackName(), joined.myColor(), chatPanel);
        dispose();
    }

    private JButton createMenuButton(String text, String iconPath) {
        JButton btn = new JButton(text);

        btn.setFocusable(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(55, 65, 81));
        btn.setPreferredSize(new Dimension(250, 60));

        btn.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(70, 80, 95), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                )
        );

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image img = icon.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            // 아이콘 없어도 동작은 해야 함
        }

        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StartMenu::new);
    }
}
