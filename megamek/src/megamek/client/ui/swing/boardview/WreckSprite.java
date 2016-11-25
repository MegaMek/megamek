package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Entity;
import megamek.common.util.ImageUtil;

/**
 * Sprite for an wreck. Consists of an image, drawn from the Tile Manager
 * and an identification label.
 */
class WreckSprite extends Sprite {

    private Entity entity;

    private Rectangle modelRect;

    private int secondaryPos;

    public WreckSprite(BoardView1 boardView1, final Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
        this.secondaryPos = secondaryPos;

        String shortName = entity.getShortName();

        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, bv.getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getFontMetrics(font).getAscent());
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        if (secondaryPos == -1) {
            tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(bv.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }

        bounds = tempBounds;
        image = null;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        bounds = tempBounds;

        return bounds;
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        // figure out size
        String shortName = entity.getShortName();
        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        Rectangle tempRect = new Rectangle(47, 55, bv.getFontMetrics(font)
                .stringWidth(shortName) + 1, bv.getFontMetrics(font)
                .getAscent());

        // create image for buffer
        image = ImageUtil.createAcceleratedImage(bounds.width, bounds.height);
        Graphics graph = image.getGraphics();

        // Draw wreck image,if we've got one.
        Image wreck = bv.tileManager.wreckMarkerFor(entity, -1);
        if (null != wreck) {
            graph.drawImage(wreck, 0, 0, this);
        }

        if ((secondaryPos == -1) && GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.ADVANCED_DRAW_ENTITY_LABEL)) {
            // draw box with shortName
            Color text = Color.lightGray;
            Color bkgd = Color.darkGray;
            Color bord = Color.black;

            graph.setFont(font);
            graph.setColor(bord);
            graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                    tempRect.height);
            tempRect.translate(-1, -1);
            graph.setColor(bkgd);
            graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                    tempRect.height);
            graph.setColor(text);
            graph.drawString(shortName, tempRect.x + 1,
                    (tempRect.y + tempRect.height) - 1);
        }

        // create final image
        image = bv.getScaledImage(image, false);
        graph.dispose();
    }

    /**
     * Overrides to provide for a smaller sensitive area.
     */
    @Override
    public boolean isInside(Point point) {
        return false;
    }

}