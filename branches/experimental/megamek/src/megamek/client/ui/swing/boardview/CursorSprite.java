package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Coords;

/**
 * Sprite for a cursor. Just a hexagon outline in a specified color.
 */
class CursorSprite extends Sprite {

    private Color color;

    private Coords hexLoc;

    public CursorSprite(BoardView1 boardView1, final Color color) {
        super(boardView1);
        this.color = color;
        bounds = new Rectangle(this.bv.hexPoly.getBounds().width + 1,
                this.bv.hexPoly.getBounds().height + 1);
        image = null;

        // start offscreen
        setOffScreen();
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = this.bv.createImage(bounds.width, bounds.height);
        Graphics graph = tempImage.getGraphics();

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        // draw attack poly
        graph.setColor(color);
        graph.drawPolygon(this.bv.hexPoly);

        // create final image
        if (this.bv.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.bv.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        } else {
            image = this.bv.getScaledImage(this.bv.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
        }
        graph.dispose();
        tempImage.flush();
    }

    public void setOffScreen() {
        bounds.setLocation(-100, -100);
        hexLoc = new Coords(-2, -2);
    }

    public void setHexLocation(Coords hexLoc) {
        this.hexLoc = hexLoc;
        bounds.setLocation(this.bv.getHexLocation(hexLoc));
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(this.bv.hexPoly.getBounds().width + 1,
                this.bv.hexPoly.getBounds().height + 1);
        bounds.setLocation(this.bv.getHexLocation(hexLoc));

        return bounds;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}