package megamek.client.ui.swing.boardview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.client.ui.Messages;
import megamek.common.Flare;

/**
 * Sprite for a flare. Changes whenever the entity changes. Consists of an
 * image, drawn from the Tile Manager; facing and possibly secondary facing
 * arrows; armor and internal bars; and an identification label.
 */
class FlareSprite extends Sprite {

    Flare flare;  

    public FlareSprite(BoardView1 boardView1, final Flare f) {
        super(boardView1);
        flare = f;

        getBounds();
        image = boardView1.getFlareImage();
    }

    @Override
    public Rectangle getBounds() {

        Dimension dim = new Dimension(boardView1.hex_size.width, 
                boardView1.hex_size.height);
        bounds = new Rectangle(dim);
        bounds.setLocation(this.boardView1.getHexLocation(flare.position));

        return bounds;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        drawOnto(g, x, y, observer, false);
    }

    @Override
    public void prepare() {
    }

    @Override
    public String[] getTooltip() {
        String[] tipStrings = new String[1];
        tipStrings[0] = Messages.getString("BoardView1.flare", 
                new Object []{flare.turnsToBurn});
        return tipStrings;
    }
}