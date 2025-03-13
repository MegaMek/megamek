package megamek.common;

public class OffBoardHex extends Hex {

    @Override
    public boolean isOnBoard() {
        return false;
    }

    @Override
    public boolean isOffBoard() {
        return true;
    }
}
