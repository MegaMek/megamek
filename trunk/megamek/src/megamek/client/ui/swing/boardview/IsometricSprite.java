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
import megamek.common.IPlayer;
import megamek.common.options.IOptions;

/**
 * Sprite used for isometric rendering to render an entity partially hidden
 * behind a hill.
 *
 */
class IsometricSprite extends Sprite {

    Entity entity;
    private Image radarBlipImage;
    private Rectangle modelRect;
    private int secondaryPos;

    public IsometricSprite(BoardView1 boardView1, Entity entity, int secondaryPos, Image radarBlipImage) {
        super(boardView1);
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        String shortName = entity.getShortName();
        Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, bv.getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (bv.DROPSHDW_DIST * bv.scale);
        } else if (bv.useIsometric() && (entity.getElevation() != 0)
                && !(entity instanceof GunEmplacement)) {
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
        
        if (!isReady()) {
            prepare();
            return;
        }
        Point p;
        if (secondaryPos == -1) {
            p = bv.getHexLocation(entity.getPosition());
        } else {
            p = bv.getHexLocation(entity.getSecondaryPositions().get(
                    secondaryPos));
        }
        Graphics2D g2 = (Graphics2D) g;
        
        if (onlyDetectedBySensors()) {
            Image blipImage = bv.getScaledImage(radarBlipImage, true);
            if (makeTranslucent) {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.65f));
                g2.drawImage(blipImage, x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(blipImage, x, y, observer);
            }            
        } else if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(
                    entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);
            // Draw airborne units in 2 passes. Shadow is rendered
            // during the opaque pass, and the
            // Actual unit is rendered during the transparent pass.
            // However the unit is always drawn
            // opaque.
            if (makeTranslucent) {
                g.drawImage(image, p.x, p.y
                        - (int) (bv.DROPSHDW_DIST * bv.scale), this);
            } else {
                g.drawImage(shadow, p.x, p.y, this);
            }
        } else if ((entity.getElevation() != 0)
                && !(entity instanceof GunEmplacement)) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(
                    entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);

            // Entities on a bridge hex or submerged in water.
            int altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * bv.scale);
            if (makeTranslucent) {
                if (entity.relHeight() < 0) {
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
    }

    @Override
    public void prepare() {
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
        graph.drawImage(bv.tileManager.imageFor(entity, secondaryPos), 0, 0,
                this);

        // create final image
        image = bv.getScaledImage(bv.createImage(new FilteredImageSource(
                tempImage.getSource(), new KeyAlphaFilter(
                        BoardView1.TRANSPARENT))), false);
        graph.dispose();
        tempImage.flush();
    }
    
    /**
     * We only want to show double-blind visibility indicators on our own
     * mechs and teammates mechs (assuming team vision option).
     */
    private boolean trackThisEntitiesVisibilityInfo(Entity e) {
        IPlayer localPlayer = this.bv.getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }
        IOptions opts = this.bv.game.getOptions();
        if (opts.booleanOption("double_blind") //$NON-NLS-1$
                && ((e.getOwner().getId() == localPlayer.getId()) 
                        || (opts.booleanOption("team_vision") //$NON-NLS-1$
                && (e.getOwner().getTeam() == localPlayer.getTeam())))) {
            return true;
        }
        return false;
    }
    
    /**
     * Used to determine if this EntitySprite is only detected by an enemies
     * sensors and hence should only be a sensor return.
     * 
     * @return
     */
    private boolean onlyDetectedBySensors() {
        boolean sensors = bv.game.getOptions().booleanOption(
                "tacops_sensors");
        boolean sensorsDetectAll = bv.game.getOptions().booleanOption(
                "sensors_detect_all");
        boolean doubleBlind = bv.game.getOptions().booleanOption(
                "double_blind");
        boolean hasVisual = entity.hasSeenEntity(bv.getLocalPlayer());
        boolean hasDetected = entity.hasDetectedEntity(bv.getLocalPlayer());

        if (sensors && doubleBlind && !sensorsDetectAll
                && !trackThisEntitiesVisibilityInfo(entity)
                && hasDetected && !hasVisual) {
            return true;
        } else {
            return false;
        }
    }

}
