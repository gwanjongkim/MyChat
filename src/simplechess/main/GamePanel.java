package simplechess.main;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.JPanel;
import simplechess.piece.*;
import javax.swing.JOptionPane;


public class GamePanel extends JPanel implements Runnable {

    public void setMyColor(int color) {
        myColor = color;
        if (color == WHITE) {
            blackView = false;
        } else {
            blackView = true;
        }
        for (Piece p : pieces) {
            p.x = p.getX(p.col);
            p.y = p.getY(p.row);
        }
        copyPieces(pieces, simPieces);
        repaint();
    }

    public interface CheckListener {
        void onCheck(int kingColor);
    }

    private CheckListener checkListener;

    public interface TurnListener {
        void onTurnChanged(int currentColor);
    }

    public void setCheckListener(CheckListener listener) {
        this.checkListener = listener;
    }

    private TurnListener turnListener;

    public void setTurnListener(TurnListener listener) {
        this.turnListener = listener;
    }

    public interface MoveListener {
        void onMoveCommitted(int fromCol, int fromRow, int toCol, int toRow, int color);
    }

    private MoveListener moveListener;

    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
    }

    public interface GameOverListener {
        void onGameOver(int winnerColor);
    }

    private GameOverListener gameOverListener;

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public void applyNetworkMove(int fromCol, int fromRow, int toCol, int toRow) {
        Piece target = null;

        for (Piece p : pieces) {
            if (p.col == fromCol && p.row == fromRow) {
                target = p;
                break;
            }
        }

        if (target == null) return;

        for (int i = pieces.size() - 1; i >= 0; i--) {
            Piece other = pieces.get(i);
            if (other != target && other.col == toCol && other.row == toRow) {
                pieces.remove(i);
            }
        }

        target.col = toCol;
        target.row = toRow;
        target.x = target.getX(toCol);
        target.y = target.getY(toRow);

        copyPieces(pieces, simPieces);

        changePlayer();
        checkCheck();

        repaint();
        checkGameOver();
    }

    public static final int WIDTH = Board.SQUARE_SIZE * 8;
    public static final int HEIGHT = Board.SQUARE_SIZE * 8;

    final int FPS = 60;
    Thread gameThread;

    Board board = new Board();
    Mouse mouse = new Mouse();

    public static boolean blackView = false;
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();

    Piece activeP, checkingP;
    public static Piece castlingP;

    public static final int WHITE = 0;
    public static final int BLACK = 1;
    private int currentColor = WHITE;
    private Piece selected = null;
    int myColor = WHITE;
    private boolean soloMode = false;

    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameover;
    boolean promotionChoiceRequested;


    public GamePanel(int myColor) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);

        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setMyColor(myColor);

        setPieces();
        copyPieces(pieces, simPieces);

        launchGame();
    }

    public GamePanel() {
        this(WHITE);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        pieces.clear();

        for (int i = 0; i < 8; i++) pieces.add(new Pawn(WHITE, i, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        for (int i = 0; i < 8; i++) pieces.add(new Pawn(BLACK, i, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }

    public static int toScreenX(int col) {
        return blackView ? (7 - col) * Board.SQUARE_SIZE : col * Board.SQUARE_SIZE;
    }

    public static int toScreenY(int row) {
        return blackView ? (7 - row) * Board.SQUARE_SIZE : row * Board.SQUARE_SIZE;
    }

    public static int toBoardColFromPixel(int x) {
        int colScreen = x / Board.SQUARE_SIZE;
        if (colScreen < 0) colScreen = 0;
        if (colScreen > 7) colScreen = 7;
        return blackView ? 7 - colScreen : colScreen;
    }

    public static int toBoardRowFromPixel(int y) {
        int rowScreen = y / Board.SQUARE_SIZE;
        if (rowScreen < 0) rowScreen = 0;
        if (rowScreen > 7) rowScreen = 7;
        return blackView ? 7 - rowScreen : rowScreen;
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (Piece p : source) target.add(p);
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (gameover) return;

        if (promotion) {
            promoting();
            return;
        }
        if (!soloMode && currentColor != myColor) return;

        if (mouse.pressed) {
            if (activeP == null) {
                int col = toBoardColFromPixel(mouse.x);
                int row = toBoardRowFromPixel(mouse.y);

                for (Piece piece : simPieces) {
                    if (piece.color == currentColor &&
                            piece.col == col &&
                            piece.row == row) {
                        activeP = piece;
                        break;
                    }
                }
            } else simulate();
        }

        if (!mouse.pressed) {
            if (activeP != null) {
                if (validSquare) {
                    int fromCol = activeP.preCol;
                    int fromRow = activeP.preRow;
                    int toCol = activeP.col;
                    int toRow = activeP.row;

                    copyPieces(simPieces, pieces);
                    activeP.updatePosition();
                    if (castlingP != null) castlingP.updatePosition();

                    if (moveListener != null)
                        moveListener.onMoveCommitted(fromCol, fromRow, toCol, toRow, currentColor);

                    checkCheck();

                    if (canPromote()) {
                        promotion = true;
                        promotionChoiceRequested = false; // 프로모션 시작할 때만 입력 받게 초기화
                        promotionPawn = activeP;
                    } else {
                        changePlayer();
                        checkGameOver();
                    }
                } else {
                    copyPieces(pieces, simPieces);
                    activeP.resetPosition();
                }
                if (!promotion) activeP = null;
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        copyPieces(pieces, simPieces);

        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = toBoardColFromPixel(mouse.x);
        activeP.row = toBoardRowFromPixel(mouse.y);

        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;

            if (activeP.hittingP != null)
                simPieces.remove(activeP.hittingP.getIndex());

            checkCastling();

            if (!isIllegal(activeP)) validSquare = true;
        }
    }

    private boolean isIllegal(Piece king) {
        if (king.type == Type.KING) {
            for (Piece piece : simPieces) {
                if (piece != king && piece.color != king.color &&
                        piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkCheck() {
        for (Piece king : pieces) {
            if (king.type == Type.KING) {
                for (Piece p : pieces) {
                    if (p.color != king.color &&
                            p.canMove(king.col, king.row)) {
                        if (checkListener != null)
                            checkListener.onCheck(king.color);
                        return;
                    }
                }
            }
        }
    }

    private boolean canPromote() {
        if (activeP.type == Type.PAWN)
            return (currentColor == WHITE && activeP.row == 0) ||
                    (currentColor == BLACK && activeP.row == 7);
        return false;
    }

    private Piece promotionPawn;

    //프로모션 구현
    private void promoting() {
        // update()가 매 프레임 호출되므로 입력창이 계속 뜨는 걸 방지
        if (promotionChoiceRequested) return;
        promotionChoiceRequested = true;

        System.out.println("프로모션! 1번 퀸 2번 비숍 3번 나이트 4번 룩");

        String input = JOptionPane.showInputDialog(
                this,
                "프로모션!\n1=퀸 2=비숍 3=나이트 4=룩\n번호(1~4)를 입력하세요:"
        );

        int choice = 1; // 기본: 퀸
        try {
            if (input != null) choice = Integer.parseInt(input.trim());
        } catch (Exception ignored) {}

        switch (choice) {
            case 4 -> simPieces.add(new Rook(currentColor, activeP.col, activeP.row));
            case 3 -> simPieces.add(new Knight(currentColor, activeP.col, activeP.row));
            case 2 -> simPieces.add(new Bishop(currentColor, activeP.col, activeP.row));
            case 1 -> simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
            default -> simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
        }


        // 기존 폰 제거 + 실제 보드 반영
        simPieces.remove(activeP.getIndex());
        copyPieces(simPieces, pieces);

        activeP = null;
        promotion = false;
        promotionChoiceRequested = false;

        changePlayer();
        checkGameOver();
    }


    private void changePlayer() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        activeP = null;

        if (turnListener != null)
            turnListener.onTurnChanged(currentColor);
    }

    private void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) castlingP.col += 3;
            else if (castlingP.col == 7) castlingP.col -= 2;
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void checkGameOver() {
        boolean whiteKingAlive = false;
        boolean blackKingAlive = false;

        for (Piece p : pieces) {
            if (p.type == Type.KING) {
                if (p.color == WHITE) whiteKingAlive = true;
                if (p.color == BLACK) blackKingAlive = true;
            }
        }

        if (!whiteKingAlive || !blackKingAlive) {
            gameover = true;
            int winner = whiteKingAlive ? WHITE : BLACK;

            if (gameOverListener != null)
                gameOverListener.onGameOver(winner);

            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        board.draw(g2);

        for (Piece p : simPieces)
            p.draw(g2);

        if (activeP != null) {
            if (canMove) {
                g2.setColor(Color.white);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(
                        toScreenX(activeP.col),
                        toScreenY(activeP.row),
                        Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE
                );
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            activeP.draw(g2);
        }
    }
}

