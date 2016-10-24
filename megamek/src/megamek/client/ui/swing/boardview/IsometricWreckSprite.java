package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Entity;
import megamek.common.util.ImageUtil;

/**
 * Sprite for an wreck. Consists of an image, drawn from the Tile Manager
 * and an identification label.
 */
class IsometricWreckSprite extends Sprite {

    private Entity entity;

    private Rectangle modelRect;

    private int secondaryPos;

    public IsometricWreckSprite(BoardView1 boardView1, final Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
        this.secondaryPos = secondaryPos;

        String shortName = entity.getShortName();

        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, bv.getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getFontMetrics(font).getAscent());
        int altAdjust = 0;
        if (bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (bv.DROPSHDW_DIST * bv.scale);
        } else if (bv.useIsometric() && (entity.getElevation() != 0)) {
            altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * bv.scale);
        }

        Dimension dim = new Dimension(bv.hex_size.width, bv.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);

        if (secondaryPos == -1) {
            tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(bv.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }
        if (entity.getElevation() > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
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
    *
    */
    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
            boolean makeTranslucent) {
        if (isReady()) {
            Graphics2D g2 = (Graphics2D) g;
            if (makeTranslucent) {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.35f));
                g2.drawImage(image, x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(image, x, y, observer);
            }
        } else {
            prepare();
        }
    }

    public Entity getEntity() {
        return entity;
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