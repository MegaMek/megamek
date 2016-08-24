package megamek.client.ui.swing.boardview;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.common.Entity;
import megamek.common.util.ImageUtil;

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
     * Creates the sprite for this entity.
     */
    @Override
    public void prepare() {
        image = ImageUtil.createAcceleratedImage(bounds.width, bounds.height);
        Graphics graph = image.getGraphics();
        graph.drawImage(bv.tileManager.imageFor(entity), 0, 0, this);
        image = bv.getScaledImage(image, false);
        graph.dispose();
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