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
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;

/**
 * Sprite used for isometric rendering to render an entity partially hidden
 * behind a hill.
 *
 */
class IsometricSprite extends Sprite {

    Entity entity;
    private Rectangle modelRect;
    private int secondaryPos;

    public IsometricSprite(BoardView1 boardView1, Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
        this.secondaryPos = secondaryPos;
        String shortName = entity.getShortName();
        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, this.boardView1.getFontMetrics(font).stringWidth(
                shortName) + 1, this.boardView1.getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (this.boardView1.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (this.boardView1.DROPSHDW_DIST * this.boardView1.scale);
        } else if (this.boardView1.useIsometric() && (entity.getElevation() != 0)
                && !(entity instanceof GunEmplacement)) {
            altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * this.boardView1.scale);
        }

        Dimension dim = new Dimension(this.boardView1.hex_size.width, this.boardView1.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);

        if (secondaryPos == -1) {
            tempBounds.setLocation(this.boardView1.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(this.boardView1.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }
        if (entity.getElevation() > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
        }
        bounds = tempBounds;
        image = null;
    }

    public Coords getPosition() {
        if (secondaryPos == -1) {
            return entity.getPosition();
        } else {
            return entity.getSecondaryPositions().get(secondaryPos);
        }
    }

    /**
     *
     */
    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
            boolean makeTranslucent) {
        if (isReady()) {
            Point p;
            if (secondaryPos == -1) {
                p = this.boardView1.getHexLocation(entity.getPosition());
            } else {
                p = this.boardView1.getHexLocation(entity.getSecondaryPositions().get(
                        secondaryPos));
            }
            Graphics2D g2 = (Graphics2D) g;
            if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                Image shadow = this.boardView1.createShadowMask(this.boardView1.tileManager.imageFor(
                        entity, entity.getFacing(), secondaryPos));

                if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
                    shadow = this.boardView1.createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(
                                    BoardView1.TRANSPARENT)));
                } else {
                    shadow = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(
                                    BoardView1.TRANSPARENT))),false);
                }
                // Draw airborne units in 2 passes. Shadow is rendered
                // during the opaque pass, and the
                // Actual unit is rendered during the transparent pass.
                // However the unit is always drawn
                // opaque.
                if (makeTranslucent) {
                    g.drawImage(image, p.x, p.y
                            - (int) (this.boardView1.DROPSHDW_DIST * this.boardView1.scale), this);
                } else {
                    g.drawImage(shadow, p.x, p.y, this);
                }

            } else if ((entity.getElevation() != 0)
                    && !(entity instanceof GunEmplacement)) {
                Image shadow = this.boardView1.createShadowMask(this.boardView1.tileManager.imageFor(
                        entity, entity.getFacing(), secondaryPos));

                if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
                    shadow = this.boardView1.createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(
                                    BoardView1.TRANSPARENT)));
                } else {
                    shadow = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(
                                    BoardView1.TRANSPARENT))),false);
                }
                // Entities on a bridge hex or submerged in water.
                int altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * this.boardView1.scale);

                if (makeTranslucent) {
                    if (entity.absHeight() < 0) {
                        g2.setComposite(AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER, 0.35f));
                        g2.drawImage(image, p.x, p.y - altAdjust, observer);
                        g2.setComposite(AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER, 1.0f));
                    } else {
                        g.drawImage(image, p.x, p.y - altAdjust, this);
                    }
                } else {
                    g.drawImage(shadow, p.x, p.y, this);
                }

            } else if (makeTranslucent) {
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

    @Override
    public void prepare() {
        Image tempImage;
        Graphics graph;
        try {
            tempImage = this.boardView1.createImage(bounds.width, bounds.height);
            graph = tempImage.getGraphics();
        } catch (NullPointerException ex) {
            // argh! but I want it!
            return;
        }

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);

        // draw entity image
        graph.drawImage(this.boardView1.tileManager.imageFor(entity, secondaryPos), 0, 0,
                this);

        // create final image
        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
        }
        graph.dispose();
        tempImage.flush();
    }

}