package simplechess.piece;

import simplechess.main.GamePanel;
import simplechess.main.Type;

public class Pawn extends Piece {

    public Pawn(int color, int col, int row) {
        super(color, col, row);
        
        type = Type.PAWN;
        
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-pawn");
        }
        else {
            image = getImage("/piece/b-pawn");
        }
    }
    public boolean canMove(int targetCol, int targetRow) {

        if (isWithinBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {

            // Define the move value based on its color
            int moveValue;
            if (color == GamePanel.WHITE) {
                moveValue = -1;
            }
            else {
                moveValue = 1;
            }

            // Check the hitting piece
            hittingP = getHittingP(targetCol, targetRow);

            // 1 square movement
            if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
                return true;
            }
         // 2 squares movement
            if(targetCol == preCol && targetRow == preRow + moveValue*2
                    && hittingP == null && moved == false
                    && pieceIsOnStraightLine(targetCol, targetRow) == false) {
                return true;
            }
            if (Math.abs(targetCol - preCol) == 1 
            	    && targetRow == preRow + moveValue 
            	    && hittingP != null 
            	    && hittingP.color != color) {

            	    return true;
            	}
            
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
                for(Piece piece : GamePanel.simPieces) {
                    if(piece.col == targetCol && piece.row == preRow && piece.twoStepped == true) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

        }

        return false;
    }

}

