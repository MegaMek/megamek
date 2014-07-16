package megamek.client.ui.swing.boardview;

import java.awt.Rectangle;

import megamek.common.Coords;

/**
 * An ancestor class for all Sprites that can be enclosed within a single hex.
 * 
 * @author Saginatio
 * 
 */
public abstract class HexSprite extends Sprite {

    protected Coords loc;

    public HexSprite(BoardView1 boardView1, Coords loc) {
        super(boardView1);
        this.loc = loc;
        updateBounds();
    }

    public Coords getPosition() {
        return loc;
    }

    protected void updateBounds() {
        bounds = new Rectangle(this.boardView1.hexPoly.getBounds().width,
                this.boardView1.hexPoly.getBounds().height);
        bounds.setLocation(this.boardView1.getHexLocation(loc));
    }

}