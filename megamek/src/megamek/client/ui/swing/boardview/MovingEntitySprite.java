package megamek.client.ui.swing.boardview;

import megamek.MMConstants;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.util.ImageUtil;

import java.awt.*;
import java.awt.image.ImageObserver;

class MovingEntitySprite extends Sprite {

    private int facing;

    private Entity entity;

    private Rectangle modelRect;

    private int elevation;

    public MovingEntitySprite(BoardView boardView1, final Entity entity, final Coords position,
                              final int facing, final int elevation) {
        super(boardView1);
        this.entity = entity;
        this.facing = facing;
        this.elevation = elevation;

        String shortName = entity.getShortName();
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47, 55, bv.getPanel().getFontMetrics(font).stringWidth(shortName) + 1,
                bv.getPanel().getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (bv.DROPSHDW_DIST * bv.scale);
        } else if (bv.useIsometric() && (elevation != 0)) {
            altAdjust = (int) (elevation * BoardView.HEX_ELEV * bv.scale);
        }

        Dimension dim = new Dimension(bv.hex_size.width, bv.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);

        tempBounds.setLocation(bv.getHexLocation(position));
        if (elevation > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
        }
        bounds = tempBounds;
        image = null;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        // If this is an airborne unit, render the shadow.
        if (bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(entity,
                    facing, -1));
            shadow = bv.getScaledImage(shadow, true);

            g.drawImage(shadow, x, y + (int) (bv.DROPSHDW_DIST * bv.scale),
                    observer);
        } else if (elevation > 0) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(entity,
                    facing, -1));
            shadow = bv.getScaledImage(shadow, true);
            
            g.drawImage(shadow, x, y
                    + (int) (elevation * BoardView.HEX_ELEV * bv.scale),
                    observer);
        }
        // submerged?
        if (bv.useIsometric() && ((elevation + entity.getHeight()) < 0)) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    0.35f));
            g2.drawImage(image, x, y
                    - (int) (elevation * BoardView.HEX_ELEV * bv.scale),
                    observer);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    1.0f));
        } else {
            // create final image
            drawOnto(g, x, y, observer, false);
        }
        // If this is a submerged unit, render the shadow after the unit.
        if (bv.useIsometric() && (elevation < 0)) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(entity,
                    facing, -1));
            shadow = bv.getScaledImage(shadow, true);         

            g.drawImage(shadow, x, y, observer);
        }
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        image = ImageUtil.createAcceleratedImage(bounds.width, bounds.height);
        Graphics graph = image.getGraphics();
        graph.drawImage(bv.tileManager.imageFor(entity, facing, -1), 0, 0,
                this);
        image = bv.getScaledImage(image, false);
        graph.dispose();
    }
}