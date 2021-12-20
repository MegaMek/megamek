package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Coords;

/**
 * Sprite for a cursor. Just a hexagon outline in a specified color.
 */
class CursorSprite extends Sprite {

    private Color color;

    private Coords hexLoc;

    public CursorSprite(BoardView boardView1, final Color color) {
        super(boardView1);
        this.color = color;
        bounds = new Rectangle(BoardView.hexPoly.getBounds().width + 1,
                BoardView.hexPoly.getBounds().height + 1);
        image = null;

        // start offscreen
        setOffScreen();
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = new BufferedImage(bounds.width, bounds.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics graph = tempImage.getGraphics();
        
        if (GUIPreferences.getInstance().getAntiAliasing()) {
            ((Graphics2D) graph).setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // fill with key color
        graph.setColor(new Color(0,0,0,0));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        // draw attack poly
        graph.setColor(color);
        graph.drawPolygon(BoardView.hexPoly);

        // create final image
        image = bv.getScaledImage(bv.createImage(tempImage.getSource()), false);
        
        graph.dispose();
        tempImage.flush();
    }

    public void setOffScreen() {
        bounds.setLocation(-100, -100);
        hexLoc = new Coords(-2, -2);
    }
    
    public boolean isOffScreen() {
        return !bv.game.getBoard().contains(hexLoc);
    }

    public void setHexLocation(Coords hexLoc) {
        this.hexLoc = hexLoc;
        bounds.setLocation(bv.getHexLocation(hexLoc));
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(BoardView.hexPoly.getBounds().width + 1,
                BoardView.hexPoly.getBounds().height + 1);
        bounds.setLocation(bv.getHexLocation(hexLoc));

        return bounds;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    @Override
    public boolean isHidden() {
        return hidden || isOffScreen();
    }
}