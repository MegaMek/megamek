package megamek.client.ui.swing.boardview;

import megamek.common.Coords;

public abstract class HexSprite extends Sprite {

    protected Coords loc;

    public HexSprite(BoardView1 boardView1, Coords loc) {
        super(boardView1);
        this.loc = loc;
    }

    public Coords getPosition() {
        return loc;
    }

}