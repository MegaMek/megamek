package megamek.client.ui.swing.boardview;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;

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
        bounds = new Rectangle(
                (int)(BoardView1.HEX_W*bv.scale),
                (int)(BoardView1.HEX_H*bv.scale));
        bounds.setLocation(bv.getHexLocation(loc));
    }
    
    /**
     * Creates a new empty transparent image for this HexSprite. The
     * size follows the current values of <code>bounds</code>. 
     */
    protected Image createNewHexImage() {
        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();

        // a compatible image should be ideal for blitting to the screen
        return config.createCompatibleImage(bounds.width, bounds.height,
                Transparency.TRANSLUCENT);
    }

}
