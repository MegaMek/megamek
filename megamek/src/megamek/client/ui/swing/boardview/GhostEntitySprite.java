package megamek.client.ui.swing.boardview;

import megamek.MMConstants;
import megamek.common.Entity;
import megamek.common.util.ImageUtil;

import java.awt.*;
import java.awt.image.ImageObserver;

class GhostEntitySprite extends Sprite {

    private Entity entity;

    private Rectangle modelRect;

    public GhostEntitySprite(BoardView boardView1, final Entity entity) {
        super(boardView1);
        this.entity = entity;

        String shortName = entity.getShortName();
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47, 55, bv.getPanel().getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getPanel().getFontMetrics(font).getAscent());
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