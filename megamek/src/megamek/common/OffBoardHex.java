package megamek.common;

public class OffBoardHex extends Hex {

    OffBoardHex() {
        super();
    }

    @Override
    public boolean isOnBoard() {
        return false;
    }

    @Override
    public boolean isOffBoard() {
        return true;
    }
}
