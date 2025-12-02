package common;

public final class MoveMessage implements ChatMessage {

    private final String from;
    private final int fromCol;
    private final int fromRow;
    private final int toCol;
    private final int toRow;
    private final int color;

    public MoveMessage(String from, int fromCol, int fromRow, int toCol, int toRow, int color) {
        this.from = from;
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.color = color;
    }

    @Override
    public String type() { return "move"; }

    @Override
    public String from() { return from; }

    public int fromCol() { return fromCol; }
    public int fromRow() { return fromRow; }
    public int toCol() { return toCol; }
    public int toRow() { return toRow; }
    public int color() { return color; }
}
