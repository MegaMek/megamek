package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Coords;
import megamek.common.Entity;

class MovingEntitySprite extends Sprite {

    private int facing;

    private Entity entity;

    private Rectangle modelRect;

    private int elevation;

    public MovingEntitySprite(BoardView1 boardView1, final Entity entity, final Coords position,
            final int facing, final int elevation) {
        super(boardView1);
        this.entity = entity;
        this.facing = facing;
        this.elevation = elevation;

        String shortName = entity.getShortName();
        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, this.bv.getFontMetrics(font).stringWidth(
                shortName) + 1, this.bv.getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (this.bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (this.bv.DROPSHDW_DIST * this.bv.scale);
        } else if (this.bv.useIsometric() && (elevation != 0)) {
            altAdjust = (int) (elevation * BoardView1.HEX_ELEV * this.bv.scale);
        }

        Dimension dim = new Dimension(this.bv.hex_size.width, this.bv.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);

        tempBounds.setLocation(this.bv.getHexLocation(position));
        if (elevation > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
        }
        bounds = tempBounds;
        image = null;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        // If this is an airborne unit, render the shadow.
        if (this.bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            Image shadow = this.bv.createShadowMask(this.bv.tileManager.imageFor(entity,
                    facing, -1));

            if (this.bv.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
                shadow = this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
            } else {
                shadow = this.bv.getScaledImage(this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
            }

            g.drawImage(shadow, x, y + (int) (this.bv.DROPSHDW_DIST * this.bv.scale),
                    observer);
        } else if (elevation > 0) {
            Image shadow = this.bv.createShadowMask(this.bv.tileManager.imageFor(entity,
                    facing, -1));

            if (this.bv.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
                shadow = this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
            } else {
                shadow = this.bv.getScaledImage(this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
            }

            g.drawImage(shadow, x,
                    y + (int) (elevation * BoardView1.HEX_ELEV * this.bv.scale), observer);
        }
        // submerged?
        if (this.bv.useIsometric() && ((elevation + entity.getHeight()) < 0)) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.35f));
            g2.drawImage(image, x,
                    y - (int) (elevation * BoardView1.HEX_ELEV * this.bv.scale), observer);
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
        } else {
            // create final image
            drawOnto(g, x, y, observer, false);
        }
        // If this is a submerged unit, render the shadow after the unit.
        if (this.bv.useIsometric() && (elevation < 0)) {
            Image shadow = this.bv.createShadowMask(this.bv.tileManager.imageFor(entity,
                    facing, -1));

            if (this.bv.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
                shadow = this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
            } else {
                shadow = this.bv.getScaledImage(this.bv.createImage(new FilteredImageSource(
                        shadow.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
            }

            g.drawImage(shadow, x, y, observer);
        }
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
            tempImage = this.bv.createImage(bounds.width, bounds.height);
            graph = tempImage.getGraphics();
        } catch (NullPointerException ex) {
            // argh! but I want it!
            return;
        }

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        graph.drawImage(this.bv.tileManager.imageFor(entity, facing, -1), 0, 0,
                this);

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
}