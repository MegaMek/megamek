package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Entity;

class GhostEntitySprite extends Sprite {

    private Entity entity;

    private Rectangle modelRect;

    public GhostEntitySprite(BoardView1 boardView1, final Entity entity) {
        super(boardView1);
        this.entity = entity;

        String shortName = entity.getShortName();
        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, bv.getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getFontMetrics(font).getAscent());
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));

        bounds = tempBounds;
        image = null;
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage;
        Graphics graph;
        try {
            tempImage = bv.createImage(bounds.width, bounds.height);
            graph = tempImage.getGraphics();
        } catch (NullPointerException ex) {
            // argh! but I want it!
            return;
        }

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);

        // draw entity image
        graph.drawImage(bv.tileManager.imageFor(entity), 0, 0, this);

        // create final image
        image = bv.getScaledImage(bv.createImage(new FilteredImageSource(
                tempImage.getSource(), new KeyAlphaFilter(
                        BoardView1.TRANSPARENT))), false);
        graph.dispose();
        tempImage.flush();
    }

    @Override
    public Rectangle getBounds() {
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        bounds = tempBounds;

        return bounds;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        drawOnto(g, x, y, observer, true);
    }

}