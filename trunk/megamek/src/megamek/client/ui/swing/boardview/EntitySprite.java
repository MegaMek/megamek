package megamek.client.ui.swing.boardview;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.GunEmplacement;
import megamek.common.IGame.Phase;
import megamek.common.IBoard;
import megamek.common.IPlayer;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.preference.PreferenceManager;


/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an
 * image, drawn from the Tile Manager; facing and possibly secondary facing
 * arrows; armor and internal bars; and an identification label.
 */
class EntitySprite extends Sprite {

    Entity entity;

    private Image radarBlipImage;

    private Rectangle entityRect;

    private Rectangle modelRect;

    private int secondaryPos;

    public EntitySprite(BoardView1 boardView1, final Entity entity, int secondaryPos, Image radarBlipImage) {
        super(boardView1);
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;

        String shortName = entity.getShortName();

        if (entity.getMovementMode() == EntityMovementMode.VTOL) {
            shortName = shortName.concat(" (FL: ")
                    .concat(Integer.toString(entity.getElevation()))
                    .concat(")");
        }
        if (entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
            shortName = shortName.concat(" (Depth: ")
                    .concat(Integer.toString(entity.getElevation()))
                    .concat(")");
        }
        int face = entity.isCommander() ? Font.ITALIC : Font.PLAIN;
        if (onlyDetectedBySensors()) {
            shortName = Messages.getString("BoardView1.sensorReturn") ;
            face = Font.PLAIN;
        }
        Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
        modelRect = new Rectangle(47, 55, this.bv.getFontMetrics(font).stringWidth(
                shortName) + 1, this.bv.getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (this.bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (this.bv.DROPSHDW_DIST * this.bv.scale);
        } else if (this.bv.useIsometric() && (entity.getElevation() != 0)
                && !(entity instanceof GunEmplacement)) {
            altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * this.bv.scale);
        }

        Dimension dim = new Dimension(this.bv.hex_size.width, this.bv.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);
        if (secondaryPos == -1) {
            tempBounds.setLocation(this.bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(this.bv.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }

        if (entity.getElevation() > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
        }
        bounds = tempBounds;
        entityRect = new Rectangle(bounds.x + (int) (20 * this.bv.scale), bounds.y
                + (int) (14 * this.bv.scale), (int) (44 * this.bv.scale),
                (int) (44 * this.bv.scale));
        image = null;
    }

    @Override
    public Rectangle getBounds() {

        int altAdjust = 0;
        if (this.bv.useIsometric()
                && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (this.bv.DROPSHDW_DIST * this.bv.scale);
        } else if (this.bv.useIsometric() && (entity.getElevation() != 0)
                && !(entity instanceof GunEmplacement)) {
            altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * this.bv.scale);
        }

        Dimension dim = new Dimension(this.bv.hex_size.width, this.bv.hex_size.height
                + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);
        if (secondaryPos == -1) {
            tempBounds.setLocation(this.bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(this.bv.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }
        if (entity.getElevation() > 0) {
            tempBounds.y = tempBounds.y - altAdjust;
        }
        bounds = tempBounds;
        entityRect = new Rectangle(bounds.x + (int) (20 * this.bv.scale), bounds.y
                + (int) (14 * this.bv.scale), (int) (44 * this.bv.scale),
                (int) (44 * this.bv.scale));

        return bounds;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        boolean translucentHiddenUnits = GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS);

        if ((trackThisEntitiesVisibilityInfo(entity)
                && !entity.isVisibleToEnemy() && translucentHiddenUnits)
                || (entity.relHeight() < 0)) {
            // create final image with translucency
            drawOnto(g, x, y, observer, true);
        } else {
            drawOnto(g, x, y, observer, false);
        }
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        final IBoard board = bv.game.getBoard();
        
        // figure out size
        String shortName = entity.getShortName();
        if (entity.getMovementMode() == EntityMovementMode.VTOL) {
            shortName = shortName.concat(" (FL: ") //$NON-NLS-1$
                    .concat(Integer.toString(entity.getElevation()))
                    .concat(")");
        }
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            shortName += (Messages.getString("BoardView1.ID") + entity.getId()); //$NON-NLS-1$
        }
        int face = entity.isCommander() ? Font.ITALIC : Font.PLAIN;
        if (onlyDetectedBySensors()) {
            shortName = Messages.getString("BoardView1.sensorReturn"); //$NON-NLS-1$
            face = Font.PLAIN;
        }
        Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
        Rectangle tempRect = new Rectangle(47, 55, this.bv.getFontMetrics(font)
                .stringWidth(shortName) + 1, this.bv.getFontMetrics(font)
                .getAscent());

        // create image for buffer
        Image tempImage;
        Graphics2D graph;
        try {
            tempImage = this.bv.createImage(bounds.width, bounds.height);
            // fill with key color
            graph = (Graphics2D)tempImage.getGraphics();
        } catch (NullPointerException ex) {
            // argh! but I want it!
            return;
        }

        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        if (!this.bv.useIsometric()) {
            // The entity sprite is drawn when the hexes are rendered.
            // So do not include the sprite info here.
            if (onlyDetectedBySensors() && !bv.useIsometric()) {
                graph.drawImage(radarBlipImage, 0, 0, this);
            } else {
                graph.drawImage(bv.tileManager.imageFor(entity, secondaryPos),
                        0, 0, this);
            }
        }
        if ((secondaryPos == -1) || (secondaryPos == 6)) {
            // draw box with shortName
            Color text, bkgd, bord;
            if (entity.isDone() && !onlyDetectedBySensors()) {
                text = Color.lightGray;
                bkgd = Color.darkGray;
                bord = Color.black;
            } else if (entity.isImmobile() && !onlyDetectedBySensors()) {
                text = Color.darkGray;
                bkgd = Color.black;
                bord = Color.lightGray;
            } else {
                text = Color.black;
                bkgd = Color.lightGray;
                bord = Color.darkGray;
            }
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

           
            // Past here, everything is drawing status that shouldn't be seen
            // on a sensor return, so we'll just quit here
            if (onlyDetectedBySensors()) {
                // create final image
                image = bv.getScaledImage(bv
                        .createImage(new FilteredImageSource(tempImage
                                .getSource(), new KeyAlphaFilter(
                                BoardView1.TRANSPARENT))), false);

                graph.dispose();
                tempImage.flush();
                return;
            }
            
            // draw facing
            graph.setColor(Color.white);
            boolean isInfantry = (entity instanceof Infantry);
            boolean isAero = (entity instanceof Aero);                
            if ((entity.getFacing() != -1)
                    && !(isInfantry && !((Infantry) entity).hasFieldGun()
                            && !((Infantry) entity).isTakingCover())
                    && !(isAero && ((Aero) entity).isSpheroid() && !board
                            .inSpace())) {
                graph.draw(bv.facingPolys[entity.getFacing()]);
            }


            // determine secondary facing for non-mechs & flipped arms
            int secFacing = entity.getFacing();
            if (!((entity instanceof Mech) || (entity instanceof Protomech))) {
                secFacing = entity.getSecondaryFacing();
            } else if (entity.getArmsFlipped()) {
                secFacing = (entity.getFacing() + 3) % 6;
            }
            // draw red secondary facing arrow if necessary
            if ((secFacing != -1) && (secFacing != entity.getFacing())) {
                graph.setColor(Color.red);
                //graph.drawPolygon(this.bv.facingPolys[secFacing]);
                graph.draw(this.bv.facingPolys[secFacing]);
            }
            if ((entity instanceof Aero) && this.bv.game.useVectorMove()) {
                for (int head : entity.getHeading()) {
                    graph.setColor(Color.red);
                    graph.draw(this.bv.facingPolys[head]);
                }
            }

            // Determine if the entity has a locked turret,
            // and if it is a gun emplacement
            boolean turretLocked = false;
            // boolean turretJammed = false;
            int crewStunned = 0;
            boolean ge = false;
            if (entity instanceof Tank) {
                turretLocked = !((Tank) entity).hasNoTurret()
                        && !entity.canChangeSecondaryFacing();
                crewStunned = ((Tank) entity).getStunnedTurns();
                ge = entity instanceof GunEmplacement;
            }

            // draw condition strings

            // draw elevation/altitude if non-zero
            if (entity.isAirborne()) {
                if (!board.inSpace()) {
                    graph.setColor(Color.darkGray);
                    graph.drawString(Integer.toString(entity.getAltitude())
                            + "A", 26, 15);
                    graph.setColor(Color.PINK);
                    graph.drawString(Integer.toString(entity.getAltitude())
                            + "A", 25, 14);
                }
            } else if (entity.getElevation() != 0) {
                graph.setColor(Color.darkGray);
                graph.drawString(Integer.toString(entity.getElevation()),
                        26, 15);
                graph.setColor(Color.PINK);
                graph.drawString(Integer.toString(entity.getElevation()),
                        25, 14);
            }

            if (entity instanceof Aero) {
                Aero a = (Aero) entity;

                if (a.isRolled()) {
                    // draw "rolled"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.ROLLED"), 18, 15); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.ROLLED"), 17, 14); //$NON-NLS-1$
                }

                if (a.isOutControlTotal() & a.isRandomMove()) {
                    // draw "RANDOM"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.RANDOM"), 18, 35); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.RANDOM"), 17, 34); //$NON-NLS-1$
                } else if (a.isOutControlTotal()) {
                    // draw "CONTROL"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.CONTROL"), 18, 39); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.CONTROL"), 17, 38); //$NON-NLS-1$
                }
                if (a.getFuel() <= 0) {
                    // draw "FUEL"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.FUEL"), 18, 39); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.FUEL"), 17, 38); //$NON-NLS-1$
                }

                if (a.isEvading()) {
                    // draw "EVADE" - can't overlap with out of control
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.EVADE"), 18, 39); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.EVADE"), 17, 38); //$NON-NLS-1$
                }
            }

            if (entity.getCrew().isDead()) {
                // draw "CREW DEAD"
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.CrewDead"), 18, 39); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(
                        Messages.getString("BoardView1.CrewDead"), 17, 38); //$NON-NLS-1$
            } else if (!ge && entity.isImmobile()) {
                if (entity.isProne()) {
                    // draw "IMMOBILE" and "PRONE"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                    graph.drawString(
                            Messages.getString("BoardView1.PRONE"), 26, 48); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("BoardView1.PRONE"), 25, 47); //$NON-NLS-1$
                } else if (crewStunned > 0) {
                    // draw IMMOBILE and STUNNED
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                    graph.drawString(
                            Messages.getString(
                                    "BoardView1.STUNNED", new Object[] { crewStunned }), 22, 48); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString(
                                    "BoardView1.STUNNED", new Object[] { crewStunned }), 21, 47); //$NON-NLS-1$
                } else if (turretLocked) {
                    // draw "IMMOBILE" and "LOCKED"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                    graph.drawString(
                            Messages.getString("BoardView1.LOCKED"), 22, 48); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("BoardView1.LOCKED"), 21, 47); //$NON-NLS-1$
                } else {
                    // draw "IMMOBILE"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 18, 39); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.IMMOBILE"), 17, 38); //$NON-NLS-1$
                }
            } else if (entity.isProne()) {
                // draw "PRONE"
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.PRONE"), 26, 39); //$NON-NLS-1$
                graph.setColor(Color.yellow);
                graph.drawString(
                        Messages.getString("BoardView1.PRONE"), 25, 38); //$NON-NLS-1$
            } else if (crewStunned > 0) {
                // draw STUNNED
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString(
                                "BoardView1.STUNNED", new Object[] { crewStunned }), 22, 48); //$NON-NLS-1$
                graph.setColor(Color.yellow);
                graph.drawString(
                        Messages.getString(
                                "BoardView1.STUNNED", new Object[] { crewStunned }), 21, 47); //$NON-NLS-1$
            } else if (turretLocked) {
                // draw "LOCKED"
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.LOCKED"), 22, 39); //$NON-NLS-1$
                graph.setColor(Color.yellow);
                graph.drawString(
                        Messages.getString("BoardView1.LOCKED"), 21, 38); //$NON-NLS-1$
            } else if ((entity.getGrappled() != Entity.NONE) 
                    && entity.isGrappleAttacker()) {
             // draw "GRAPPLED"
                graph.setColor(Color.black);
                graph.drawString(
                        Messages.getString("BoardView1.GRAPPLER"), 22, 39); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(
                        Messages.getString("BoardView1.GRAPPLER"), 21, 38); //$NON-NLS-1$
            } else if ((entity.getGrappled() != Entity.NONE) ) {
             // draw "GRAPPLED"
                graph.setColor(Color.black);
                graph.drawString(
                        Messages.getString("BoardView1.GRAPPLED"), 22, 39); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(
                        Messages.getString("BoardView1.GRAPPLED"), 21, 38); //$NON-NLS-1$
            }

            // If this unit is shutdown, say so.
            if (entity.isManualShutdown()) {
                // draw "SHUTDOWN" for manual
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.SHUTDOWN"), 50, 71); //$NON-NLS-1$
                graph.setColor(Color.yellow);
                graph.drawString(
                        Messages.getString("BoardView1.SHUTDOWN"), 49, 70); //$NON-NLS-1$
            } else if (entity.isShutDown()) {
                // draw "SHUTDOWN" for manual
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.SHUTDOWN"), 50, 71); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(
                        Messages.getString("BoardView1.SHUTDOWN"), 49, 70); //$NON-NLS-1$
            }

            // If this unit is being swarmed or is swarming another, say so.
            if (Entity.NONE != entity.getSwarmAttackerId()) {
                // draw "SWARMED"
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.SWARMED"), 17, 22); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(
                        Messages.getString("BoardView1.SWARMED"), 16, 21); //$NON-NLS-1$
            }

            // If this unit is transporting another, say so.
            if ((entity.getLoadedUnits()).size() > 0) {
                // draw "T"
                graph.setColor(Color.darkGray);
                graph.drawString("T", 20, 71); //$NON-NLS-1$
                graph.setColor(Color.black);
                graph.drawString("T", 19, 70); //$NON-NLS-1$
            }

            // If this unit is stuck, say so.
            if ((entity.isStuck())) {
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("BoardView1.STUCK"), 26, 61); //$NON-NLS-1$
                graph.setColor(Color.orange);
                graph.drawString(
                        Messages.getString("BoardView1.STUCK"), 25, 60); //$NON-NLS-1$

            }

            // If this unit is currently unknown to the enemy, say so.
            if (trackThisEntitiesVisibilityInfo(entity)) {
                if (!entity.isEverSeenByEnemy()) {
                    // draw "U"
                    graph.setColor(Color.darkGray);
                    graph.drawString("U", 30, 71); //$NON-NLS-1$
                    graph.setColor(Color.black);
                    graph.drawString("U", 29, 70); //$NON-NLS-1$
                } else if (!entity.isVisibleToEnemy()
                        && !GUIPreferences
                                .getInstance()
                                .getBoolean(
                                        GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS)) {
                    // If this unit is currently hidden from the enemy, say
                    // so.
                    // draw "H"
                    graph.setColor(Color.darkGray);
                    graph.drawString("H", 30, 71); //$NON-NLS-1$
                    graph.setColor(Color.black);
                    graph.drawString("H", 29, 70); //$NON-NLS-1$
                }
            }

            // If hull down, show
            if (entity.isHullDown()) {
                // draw "D"
                graph.setColor(Color.darkGray);
                graph.drawString(
                        Messages.getString("UnitOverview.HULLDOWN"), 15, 39); //$NON-NLS-1$
                graph.setColor(Color.yellow);
                graph.drawString(
                        Messages.getString("UnitOverview.HULLDOWN"), 14, 38); //$NON-NLS-1$
            } else if (entity instanceof Infantry) {
                int dig = ((Infantry) entity).getDugIn();
                if (dig == Infantry.DUG_IN_COMPLETE) {
                    // draw "D"
                    graph.setColor(Color.black);
                    graph.drawString("Dug In", 27, 71); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString("Dug In", 26, 70); //$NON-NLS-1$
                } else if (dig != Infantry.DUG_IN_NONE) {
                    // draw "W"
                    graph.setColor(Color.black);
                    graph.drawString("Working", 23, 71); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString("Working", 22, 70); //$NON-NLS-1$
                } else if (((Infantry)entity).isTakingCover()) {
                    graph.setColor(Color.black);
                    String msg = Messages.getString("BoardView1.TakingCover");
                    graph.drawString(msg, 23, 71); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(msg, 22, 70); //$NON-NLS-1$
                }
            }

            // Lets draw our armor and internal status bars
            int baseBarLength = 23;
            int barLength = 0;
            double percentRemaining = 0.00;

            percentRemaining = entity.getArmorRemainingPercent();
            barLength = (int) (baseBarLength * percentRemaining);

            graph.setColor(Color.darkGray);
            graph.fillRect(56, 7, 23, 3);
            graph.setColor(Color.lightGray);
            graph.fillRect(55, 6, 23, 3);
            graph.setColor(getStatusBarColor(percentRemaining));
            graph.fillRect(55, 6, barLength, 3);

            if (!ge) {
                // Gun emplacements don't have internal structure
                percentRemaining = entity.getInternalRemainingPercent();
                barLength = (int) (baseBarLength * percentRemaining);

                graph.setColor(Color.darkGray);
                graph.fillRect(56, 11, 23, 3);
                graph.setColor(Color.lightGray);
                graph.fillRect(55, 10, 23, 3);
                graph.setColor(getStatusBarColor(percentRemaining));
                graph.fillRect(55, 10, barLength, 3);
            }

            if (GUIPreferences.getInstance().getShowDamageLevel()) {
                Color damageColor = getDamageColor();
                if (damageColor != null) {
                    graph.setColor(damageColor);
                    graph.fillOval(20, 15, 12, 12);
                }
            }
        }

        // create final image
        image = this.bv.getScaledImage(this.bv
                .createImage(new FilteredImageSource(tempImage.getSource(),
                        new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);

        graph.dispose();
        tempImage.flush();
    }

    private Color getDamageColor() {
        switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                return Color.black;
            case Entity.DMG_HEAVY:
                return Color.red;
            case Entity.DMG_MODERATE:
                return Color.yellow;
            case Entity.DMG_LIGHT:
                return Color.green;
        }
        return null;
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
        if (this.bv.game.getOptions().booleanOption("double_blind") //$NON-NLS-1$
                && ((e.getOwner().getId() == this.bv.getLocalPlayer().getId()) || (this.bv.game
                        .getOptions().booleanOption("team_vision") //$NON-NLS-1$
                && (e.getOwner().getTeam() == this.bv.getLocalPlayer().getTeam())))) {
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

    private Color getStatusBarColor(double percentRemaining) {
        if (percentRemaining <= .25) {
            return Color.red;
        } else if (percentRemaining <= .75) {
            return Color.yellow;
        } else {
            return new Color(16, 196, 16);
        }
    }

    /**
     * Overrides to provide for a smaller sensitive area.
     */
    @Override
    public boolean isInside(Point point) {
        return entityRect.contains(point.x, point.y);
    }
    
    public Coords getPosition() {
        return entity.getPosition();
    }
    
    private StringBuffer tooltipString;
    private final boolean BR = true;
    private final boolean NOBR = false;
    
    /**
     * Adds a resource string to the entity tooltip
     * 
     * @param ttSName The resource string name. "BoardView1.Tooltip." will be added in front, so
     * "Pilot" will retrieve BoardView1.Tooltip.Pilot
     * @param startBR = true will start the string with a &lt;BR&gt;; The constants BR and NOBR can be used here. 
     * @param ttO a list of Objects to insert into the {x} places in the resource.
     */
    private void addToTT(String ttSName, boolean startBR, Object... ttO) {
        if (startBR == BR) tooltipString.append("<BR>");
        if (ttO != null)
        tooltipString.append(Messages.getString(
                "BoardView1.Tooltip."+ttSName,
                ttO));
        else
            tooltipString.append(Messages.getString(
                    "BoardView1.Tooltip."+ttSName));
    }
    
    /**
     * Adds a resource string to the entity tooltip
     * 
     * @param ttSName The resource string name. "BoardView1.Tooltip." will be added in front, so
     * "Pilot" will retrieve BoardView1.Tooltip.Pilot
     * @param startBR = true will start the string with a &lt;BR&gt;; The constants BR and NOBR can be used here. 
     */
    private void addToTT(String ttSName, boolean startBR) {
        addToTT(ttSName, startBR, (Object[]) null);
    }
    
    @Override
    public StringBuffer getTooltip() {
        
        // Tooltip info for a sensor blip
        if (onlyDetectedBySensors())
            return new StringBuffer(Messages.getString("BoardView1.sensorReturn"));

        // No sensor blip...
        Infantry thisInfantry = null;
        if (entity instanceof Infantry) thisInfantry = (Infantry) entity;
        GunEmplacement thisGunEmp = null;
        if (entity instanceof GunEmplacement) thisGunEmp = (GunEmplacement) entity;
        Aero thisAero = null;
        if (entity instanceof Aero) thisAero = (Aero) entity;
        
        tooltipString = new StringBuffer();

        // Unit Chassis and Player
        addToTT("Unit", NOBR,
                Integer.toHexString(PlayerColors.getColorRGB(
                        entity.getOwner().getColorIndex())), 
                entity.getChassis(), 
                entity.getOwner().getName());
        
        // Pilot Info
        // Nickname > Name > "Pilot"
        String pnameStr = "Pilot";

        if ((entity.getCrew().getName() != null)
                && !entity.getCrew().getName().equals("")) 
            pnameStr = entity.getCrew().getName();
        
        if ((entity.getCrew().getNickname() != null)
                && !entity.getCrew().getNickname().equals("")) 
            pnameStr = "'" + entity.getCrew().getNickname() + "'";

        addToTT("Pilot", BR,
                pnameStr, 
                entity.getCrew().getGunnery(), 
                entity.getCrew().getPiloting());

        // Pilot Status
        if (!entity.getCrew().getStatusDesc().equals(""))
            addToTT("PilotStatus", NOBR, 
                    entity.getCrew().getStatusDesc());
        
        // Pilot Advantages
        int numAdv = entity.getCrew().countOptions(
                PilotOptions.LVL3_ADVANTAGES);
        if (numAdv == 1)
            addToTT("Adv1", NOBR, numAdv);
        else if (numAdv > 1) 
            addToTT("Advs", NOBR, numAdv);
        
        // Pilot Manei Domini
        if ((entity.getCrew().countOptions(
                PilotOptions.MD_ADVANTAGES) > 0)) 
            addToTT("MD", NOBR);
        
        // Unit movement ability
        if (thisGunEmp == null) {
            addToTT("Movement", BR, entity.getWalkMP(), entity.getRunMPasString());
            if (entity.getJumpMP() > 0) tooltipString.append("/" + entity.getJumpMP());
        }
        
        // Armor and Internals
        addToTT("ArmorInternals", BR, entity.getTotalArmor(),
                entity.getTotalInternal());

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            if (entity.heat == 0) 
                addToTT("Heat0", BR);
            else 
                addToTT("Heat", BR, entity.heat);
        }

        // Actual Movement
        if (thisGunEmp == null) {
            // In the Movement Phase, unit not done
            if (!entity.isDone() && this.bv.game.getPhase() == Phase.PHASE_MOVEMENT) {
                // "Has not yet moved" only during movement phase
                addToTT("NotYetMoved", BR);
                
            // In the Movement Phase, unit is done - or in the Firing Phase
            } else if (
                    (entity.isDone() && this.bv.game.getPhase() == Phase.PHASE_MOVEMENT) 
                    || this.bv.game.getPhase() == Phase.PHASE_FIRING) {
                
                // Unit didn't move
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    addToTT("NoMove", BR);
                    
                // Unit did move
                } else {
                    // Colored arrow
                    // get the color resource
                    String guipName = "AdvancedMoveDefaultColor";
                    if ((entity.moved == EntityMovementType.MOVE_RUN)
                            || (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
                            || (entity.moved == EntityMovementType.MOVE_OVER_THRUST)) 
                        guipName = "AdvancedMoveRunColor";
                    else if (entity.moved == EntityMovementType.MOVE_SPRINT) 
                        guipName = "AdvancedMoveSprintColor";
                    else if (entity.moved == EntityMovementType.MOVE_JUMP) 
                        guipName = "AdvancedMoveJumpColor";

                    // HTML color String from Preferences
                    String moveTypeColor = Integer
                            .toHexString(GUIPreferences.getInstance()
                                    .getColor(guipName).getRGB() & 0xFFFFFF);

                    // Arrow
                    addToTT("Arrow", BR, moveTypeColor);

                    // Actual movement and modifier
                    addToTT("MovementF", NOBR,
                            entity.getMovementString(entity.moved),
                            entity.delta_distance,
                            Compute.getTargetMovementModifier(this.bv.game,entity.getId()).getValue());
                }
                // Special Moves
                if (entity.isEvading()) 
                    addToTT("Evade", NOBR);
                
                if ((thisInfantry != null) && (thisInfantry.isTakingCover())) 
                    addToTT("TakingCover", NOBR);

                if (entity.isCharging()) 
                    addToTT("Charging", NOBR);
                
                if (entity.isMakingDfa()) 
                    addToTT("DFA", NOBR);
            }
        }
        
        // ASF Velocity
        if (thisAero != null) {
            addToTT("AeroVelocity", BR, thisAero.getCurrentVelocity());
        }
            
        // Gun Emplacement Status
        if (thisGunEmp != null) {  
            if (thisGunEmp.isTurret() && thisGunEmp.isTurretLocked(thisGunEmp.getLocTurret())) 
                addToTT("TurretLocked", BR);
        }
       
        // Unit Immobile
        if ((thisGunEmp == null) && (entity.isImmobile()))
            addToTT("Immobile", BR);
        
        // Weapon List
        if (GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {

            ArrayList<Mounted> weapons = entity.getWeaponList();
            HashMap<String, Integer> wpNames = new HashMap<String,Integer>();

            // Gather names, counts, Clan/IS
            // When clan then the number will be stored as negative
            for (Mounted curWp: weapons) {
                String weapDesc = curWp.getDesc();
                // Append ranges
                WeaponType wtype = (WeaponType)curWp.getType();
                int ranges[];
                if (entity instanceof Aero) {
                    ranges = wtype.getATRanges();
                } else {
                    ranges = wtype.getRanges(curWp);
                }
                String rangeString = "(";
                if ((ranges[RangeType.RANGE_MINIMUM] != WeaponType.WEAPON_NA) 
                        && (ranges[RangeType.RANGE_MINIMUM] != 0)) {
                    rangeString += ranges[RangeType.RANGE_MINIMUM] + "/";
                } else {
                    rangeString += "-/";
                }
                int maxRange = RangeType.RANGE_LONG;
                if (bv.game.getOptions().booleanOption(
                        OptionsConstants.AC_TAC_OPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }
                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString += ranges[i];
                    if (i != maxRange) {
                        rangeString += "/";
                    }
                }
                
                weapDesc += rangeString + ")";
                if (wpNames.containsKey(weapDesc)) {
                    int number = wpNames.get(weapDesc);
                    if (number > 0) 
                        wpNames.put(weapDesc, number + 1);
                    else 
                        wpNames.put(weapDesc, number - 1);
                } else {
                    WeaponType wpT = ((WeaponType)curWp.getType());

                    if (entity.isClan() && TechConstants.isClan(wpT.getTechLevel(entity.getYear()))) 
                        wpNames.put(weapDesc, -1);
                    else
                        wpNames.put(weapDesc, 1);
                }
            }

            // Print to Tooltip
            tooltipString.append("<FONT SIZE=\"-2\">");

            for (Entry<String, Integer> entry : wpNames.entrySet()) {
                // Check if weapon is destroyed, text gray and strikethrough if so, remove the "x "/"*"
                // Also remove "+", means currently selected for firing
                boolean wpDest = false;
                String nameStr = entry.getKey();
                if (entry.getKey().startsWith("x ")) { 
                    nameStr = entry.getKey().substring(2, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("*")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("+")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    nameStr = nameStr.concat(" <I>(Firing)</I>");
                }

                // normal coloring 
                tooltipString.append("<FONT COLOR=#8080FF>");
                // but: color gray and strikethrough when weapon destroyed
                if (wpDest) tooltipString.append("<FONT COLOR=#a0a0a0><S>");

                String clanStr = "";
                if (entry.getValue() < 0) clanStr = Messages.getString("BoardView1.Tooltip.Clan");

                // when more than 5 weapons are present, they will be grouped
                // and listed with a multiplier
                if (weapons.size() > 5) {
                    addToTT("WeaponN", BR, Math.abs(entry.getValue()), clanStr, nameStr);

                } else { // few weapons: list each weapon separately
                    for (int i = 0; i < Math.abs(entry.getValue()); i++) {
                        addToTT("Weapon", BR, Math.abs(entry.getValue()), clanStr, nameStr);
                    }
                }
                // Weapon destroyed? End strikethrough
                if (wpDest) tooltipString.append("</S>");
                tooltipString.append("</FONT>"); 
            }
            tooltipString.append("</FONT>");
        }
        return tooltipString;
    }
    
    public String getPlayerColor() {
        if (onlyDetectedBySensors()) {
            return "C0C0C0";
        } else {
            return Integer.toHexString(PlayerColors.getColorRGB(entity
                    .getOwner().getColorIndex()));
        }
    }
}

