package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

/**
 * Everything in the main map view is either the board or it's a sprite
 * displayed on top of the board. Most sprites store a transparent image
 * which they draw onto the screen when told to. Sprites keep a bounds
 * rectangle, so it's easy to tell when they return onscreen.
 */
abstract class Sprite implements ImageObserver {

    protected final BoardView1 bv;
    protected Rectangle bounds;
    protected Image image;
    // Set this to true if you don't want the sprite to be drawn.
    protected boolean hidden = false;

    Sprite(BoardView1 boardView1) {
        bv = boardView1;
    }

    /**
     * Do any necessary preparation. This is called after creation, but
     * before drawing, when a device context is ready to draw with.
     */
    public abstract void prepare();

    /**
     * When we draw our buffered images, it's necessary to implement the
     * ImageObserver interface. This provides the necessary functionality.
     */
    public boolean imageUpdate(Image image, int infoflags, int x, int y,
            int width, int height) {
        if (infoflags == ImageObserver.ALLBITS) {
            prepare();
            bv.repaint();
            return false;
        }
        return true;
    }

    /**
     * Returns our bounding rectangle. The coordinates here are stored with
     * the top left corner of the _board_ being 0, 0, so these do not always
     * correspond to screen coordinates.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Are we ready to draw? By default, checks to see that our buffered
     * image has been created.
     */
    public boolean isReady() {
        return image != null;
    }

    /**
     * Draws this sprite onto the specified graphics context.
     */
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        drawOnto(g, x, y, observer, false);
    }

    public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
            boolean makeTranslucent) {
        if (isReady()) {
            if (makeTranslucent) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(image, x, y, observer);
                g2.setComposite(AlphaComposite.SrcOver);
            } else {
                g.drawImage(image, x, y, observer);
            }
        } else {
            // grrr... we'll be ready next time!
            prepare();
        }
    }

    /**
     * Returns true if the point is inside this sprite. Uses board
     * coordinates, not screen coordinates. By default, just checks our
     * bounding rectangle, though some sprites override this for a smaller
     * sensitive area.
     */
    public boolean isInside(Point point) {
        return bounds.contains(point);
    }

    /**
     * Since most sprites being drawn correspond to something in the game,
     * this returns a little info for a tooltip.
     */
    public StringBuffer getTooltip() {
        return null;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean h) {
        hidden = h;
    }
}