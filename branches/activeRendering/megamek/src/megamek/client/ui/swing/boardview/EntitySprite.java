package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.options.PilotOptions;
import megamek.common.preference.PreferenceManager;

/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an
 * image, drawn from the Tile Manager; facing and possibly secondary facing
 * arrows; armor and internal bars; and an identification label.
 */
class EntitySprite extends Sprite {

    Entity entity;

    private Rectangle entityRect;

    private Rectangle modelRect;

    private int secondaryPos;

    public EntitySprite(BoardView1 boardView1, final Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
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
        Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
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
        entityRect = new Rectangle(bounds.x + (int) (20 * this.boardView1.scale), bounds.y
                + (int) (14 * this.boardView1.scale), (int) (44 * this.boardView1.scale),
                (int) (44 * this.boardView1.scale));
        image = null;
    }

    @Override
    public Rectangle getBounds() {

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
        entityRect = new Rectangle(bounds.x + (int) (20 * this.boardView1.scale), bounds.y
                + (int) (14 * this.boardView1.scale), (int) (44 * this.boardView1.scale),
                (int) (44 * this.boardView1.scale));

        return bounds;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        if ((trackThisEntitiesVisibilityInfo(entity)
                && !entity.isVisibleToEnemy() && GUIPreferences
                .getInstance().getBoolean(
                        GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS))
                || (entity.absHeight() < 0)) {
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
        // figure out size
        String shortName = entity.getShortName();
        if (entity.getMovementMode() == EntityMovementMode.VTOL) {
            shortName = shortName.concat(" (FL: ")
                    .concat(Integer.toString(entity.getElevation()))
                    .concat(")");
        }
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            shortName += (Messages.getString("BoardView1.ID") + entity.getId()); //$NON-NLS-1$
        }
        int face = entity.isCommander() ? Font.ITALIC : Font.PLAIN;
        Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
        Rectangle tempRect = new Rectangle(47, 55, this.boardView1.getFontMetrics(font)
                .stringWidth(shortName) + 1, this.boardView1.getFontMetrics(font)
                .getAscent());

        // create image for buffer
        Image tempImage;
        Graphics graph;
        try {
            tempImage = this.boardView1.createImage(bounds.width, bounds.height);
            // fill with key color
            graph = tempImage.getGraphics();
        } catch (NullPointerException ex) {
            // argh! but I want it!
            return;
        }

        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        if (!this.boardView1.useIsometric()) {
            // The entity sprite is drawn when the hexes are rendered.
            // So do not include the sprite info here.
            graph.drawImage(this.boardView1.tileManager.imageFor(entity, secondaryPos), 0,
                    0, this);
        }
        if ((secondaryPos == -1) || (secondaryPos == 6)) {
            // draw box with shortName
            Color text, bkgd, bord;
            if (entity.isDone()) {
                text = Color.lightGray;
                bkgd = Color.darkGray;
                bord = Color.black;
            } else if (entity.isImmobile()) {
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

            // draw facing
            graph.setColor(Color.white);
            if ((entity.getFacing() != -1)
                    && !((entity instanceof Infantry) && (((Infantry) entity)
                            .getDugIn() == Infantry.DUG_IN_NONE))
                    && !((entity instanceof Aero)
                            && ((Aero) entity).isSpheroid() && !this.boardView1.game
                            .getBoard().inSpace())) {
                graph.drawPolygon(this.boardView1.facingPolys[entity.getFacing()]);
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
                graph.drawPolygon(this.boardView1.facingPolys[secFacing]);
            }
            if ((entity instanceof Aero) && this.boardView1.game.useVectorMove()) {
                for (int head : entity.getHeading()) {
                    graph.setColor(Color.red);
                    graph.drawPolygon(this.boardView1.facingPolys[head]);
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
                if (!this.boardView1.game.getBoard().inSpace()) {
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
                    graph.setColor(Color.darkGray);
                    graph.drawString("D", 40, 71); //$NON-NLS-1$
                    graph.setColor(Color.black);
                    graph.drawString("D", 39, 70); //$NON-NLS-1$
                } else if (dig != Infantry.DUG_IN_NONE) {
                    // draw "W"
                    graph.setColor(Color.darkGray);
                    graph.drawString("W", 40, 71); //$NON-NLS-1$
                    graph.setColor(Color.black);
                    graph.drawString("W", 39, 70); //$NON-NLS-1$
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
        if (this.boardView1.getLocalPlayer() == null) {
            return false;
        }

        if (this.boardView1.game.getOptions().booleanOption("double_blind") //$NON-NLS-1$
                && ((e.getOwner().getId() == this.boardView1.getLocalPlayer().getId()) || (this.boardView1.game
                        .getOptions().booleanOption("team_vision") //$NON-NLS-1$
                && (e.getOwner().getTeam() == this.boardView1.getLocalPlayer().getTeam())))) {
            return true;
        }
        return false;
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

    @Override
    public String[] getTooltip() {
        String[] tipStrings = new String[4];
        StringBuffer buffer;

        buffer = new StringBuffer();
        buffer.append(entity.getChassis()).append(" (") //$NON-NLS-1$
                .append(entity.getOwner().getName()).append(")"); //$NON-NLS-1$
        tipStrings[0] = buffer.toString();

        boolean hasNick = ((null != entity.getCrew().getNickname()) && !entity
                .getCrew().getNickname().equals(""));
        buffer = new StringBuffer();
        buffer.append(Messages.getString("BoardView1.pilot"));
        if (hasNick) {
            buffer.append(" '").append(entity.getCrew().getNickname())
                    .append("'");
        }
        buffer.append(" (").append(entity.getCrew().getGunnery())
                .append("/") //$NON-NLS-1$
                .append(entity.getCrew().getPiloting()).append(")")
                .append("; ").append(entity.getCrew().getStatusDesc());
        int numAdv = entity.getCrew().countOptions(
                PilotOptions.LVL3_ADVANTAGES);
        boolean isMD = entity.getCrew().countOptions(
                PilotOptions.MD_ADVANTAGES) > 0;
        if (numAdv > 0) {
            buffer.append(" <") //$NON-NLS-1$
                    .append(numAdv)
                    .append(Messages.getString("BoardView1.advs")); //$NON-NLS-1$
        }
        if (isMD) {
            buffer.append(Messages.getString("BoardView1.md")); //$NON-NLS-1$
        }
        tipStrings[1] = buffer.toString();

        GunEmplacement ge = null;
        if (entity instanceof GunEmplacement) {
            ge = (GunEmplacement) entity;
        }

        buffer = new StringBuffer();
        if (ge == null) {
            buffer.append(Messages.getString("BoardView1.move")) //$NON-NLS-1$
                    .append(entity.getMovementAbbr(entity.moved))
                    .append(":") //$NON-NLS-1$
                    .append(entity.delta_distance)
                    .append(" (+") //$NON-NLS-1$
                    .append(Compute.getTargetMovementModifier(this.boardView1.game,
                            entity.getId()).getValue()).append(")") //$NON-NLS-1$
                    .append(entity.isEvading() ? Messages
                            .getString("BoardView1.Evade") : "")//$NON-NLS-1$ //$NON-NLS-2$
                    .append(";") //$NON-NLS-1$
                    .append(Messages.getString("BoardView1.Heat")) //$NON-NLS-1$
                    .append(entity.heat);
            if (entity.isCharging()) {
                buffer.append(" ") //$NON-NLS-1$
                        .append(Messages.getString("BoardView1.charge1")); //$NON-NLS-1$
            }
            if (entity.isMakingDfa()) {
                buffer.append(" ") //$NON-NLS-1$
                        .append(Messages.getString("BoardView1.DFA1")); //$NON-NLS-1$
            }
        } else {
            if (ge.isTurret() && ge.isTurretLocked(ge.getLocTurret())) {
                buffer.append(Messages.getString("BoardView1.TurretLocked"));
                if (ge.getFirstWeapon() == -1) {
                    buffer.append(",");
                    buffer.append(Messages
                            .getString("BoardView1.WeaponsDestroyed"));
                }
            } else if (ge.getFirstWeapon() == -1) {
                buffer.append(Messages
                        .getString("BoardView1.WeaponsDestroyed"));
            } else {
                buffer.append(Messages.getString("BoardView1.Operational"));
            }
        }
        if (entity.isDone()) {
            buffer.append(" (")
                    .append(Messages.getString("BoardView1.done"))
                    .append(")");
        }
        tipStrings[2] = buffer.toString();

        buffer = new StringBuffer();
        if (ge == null) {
            buffer.append(Messages.getString("BoardView1.Armor")) //$NON-NLS-1$
                    .append(entity.getTotalArmor())
                    .append(Messages.getString("BoardView1.internal")) //$NON-NLS-1$
                    .append(entity.getTotalInternal());
        }
        /*
         * else { buffer.append(Messages.getString("BoardView1.cf"))
         * //$NON-NLS-1$ .append(ge.getCurrentCF()).append(
         * Messages.getString("BoardView1.turretArmor")) //$NON-NLS-1$
         * .append(ge.getCurrentTurretArmor()); }
         */
        tipStrings[3] = buffer.toString();

        return tipStrings;
    }
}