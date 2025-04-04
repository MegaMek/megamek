/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import java.awt.*;
import java.awt.image.ImageObserver;

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.EntityWreckHelper;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Player;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;

/**
 * Sprite used for isometric rendering to render an entity partially hidden behind a hill.
 */
class IsometricSprite extends HexSprite {

    Entity entity;
    private Image radarBlipImage;
    private Rectangle modelRect;
    private int secondaryPos;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public IsometricSprite(BoardView boardView1, Entity entity, int secondaryPos, Image radarBlipImage) {
        super(boardView1, secondaryPos == -1 ? entity.getPosition() : entity.getSecondaryPositions().get(secondaryPos));
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        String shortName = entity.getShortName();
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47,
              55,
              bv.getPanel().getFontMetrics(font).stringWidth(shortName) + 1,
              bv.getPanel().getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (bv.useIsometric() && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (bv.DROP_SHADOW_DISTANCE * bv.scale);
        } else if (bv.useIsometric() && (entity.getElevation() != 0) && !(entity instanceof GunEmplacement)) {
            altAdjust = (int) (entity.getElevation() * BoardView.HEX_ELEV * bv.scale);
        }

        Dimension dim = new Dimension(bv.hex_size.width, bv.hex_size.height + altAdjust);
        Rectangle tempBounds = new Rectangle(dim).union(modelRect);

        if (secondaryPos == -1) {
            tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos)));
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

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {

        if (!isReady()) {
            prepare();
            return;
        }
        Point p;
        if (secondaryPos == -1) {
            p = bv.getHexLocation(entity.getPosition());
        } else {
            p = bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos));
        }
        Graphics2D g2 = (Graphics2D) g;

        if (onlyDetectedBySensors()) {
            Image blipImage = bv.getScaledImage(radarBlipImage, true);
            if (makeTranslucent) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
                g2.drawImage(blipImage, x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(blipImage, x, y, observer);
            }
        } else if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);
            // Draw airborne units in 2 passes. Shadow is rendered
            // during the opaque pass, and the
            // Actual unit is rendered during the transparent pass.
            // However the unit is always drawn
            // opaque.
            if (makeTranslucent) {
                g.drawImage(image, p.x, p.y - (int) (bv.DROP_SHADOW_DISTANCE * bv.scale), this);
            } else {
                g.drawImage(shadow, p.x, p.y, this);
            }
        } else if ((entity.getElevation() != 0) && !(entity instanceof GunEmplacement)) {
            Image shadow = bv.createShadowMask(bv.tileManager.imageFor(entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);

            // Entities on a bridge hex or submerged in water.
            int altAdjust = (int) (entity.getElevation() * BoardView.HEX_ELEV * bv.scale);
            if (makeTranslucent) {
                if (entity.relHeight() < 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    g2.drawImage(image, p.x, p.y - altAdjust, observer);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    g.drawImage(image, p.x, p.y - altAdjust, this);
                }
            } else {
                g.drawImage(shadow, p.x, p.y, this);
            }

        } else if (makeTranslucent) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));

            drawImmobileElements(g2, x, y, observer);

            g2.drawImage(image, x, y, observer);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            drawImmobileElements(g, x, y, observer);
            g.drawImage(image, x, y, observer);
        }
    }

    /**
     * Worker function that draws "immobile" decals.
     */
    public void drawImmobileElements(Graphics graph, int x, int y, ImageObserver observer) {
        // draw the 'fuel leak' decal where appropriate
        boolean drawFuelLeak = EntityWreckHelper.displayFuelLeak(entity);

        if (drawFuelLeak) {
            Image fuelLeak = bv.getScaledImage(bv.tileManager.bottomLayerFuelLeakMarkerFor(entity), true);
            if (null != fuelLeak) {
                graph.drawImage(fuelLeak, x, y, observer);
            }
        }

        // draw the 'tires' or 'tracks' decal where appropriate
        boolean drawMotiveWreckage = EntityWreckHelper.displayMotiveDamage(entity);

        if (drawMotiveWreckage) {
            Image motiveWreckage = bv.getScaledImage(bv.tileManager.bottomLayerMotiveMarkerFor(entity), true);
            if (null != motiveWreckage) {
                graph.drawImage(motiveWreckage, x, y, observer);
            }
        }
    }

    @Override
    public void prepare() {
        updateBounds();
        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                             .getDefaultScreenDevice()
                                             .getDefaultConfiguration();
        image = config.createCompatibleImage(bounds.width, bounds.height, Transparency.TRANSLUCENT);
        Graphics2D g = (Graphics2D) image.getGraphics();

        // draw the unit icon translucent if hidden from the enemy
        // (and activated graphics setting); or submerged
        boolean translucentHiddenUnits = GUIP.getTranslucentHiddenUnits();

        if ((trackThisEntitiesVisibilityInfo(entity) && !entity.isVisibleToEnemy() && translucentHiddenUnits) ||
                  (entity.relHeight() < 0)) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
        g.drawImage(bv.getScaledImage(bv.tileManager.imageFor(entity, secondaryPos), true), 0, 0, this);
        g.dispose();
    }

    /**
     * We only want to show double-blind visibility indicators on our own meks and teammates meks (assuming team vision
     * option).
     */
    private boolean trackThisEntitiesVisibilityInfo(Entity e) {
        Player localPlayer = this.bv.getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }

        IGameOptions opts = this.bv.game.getOptions();
        return opts.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
                     ((e.getOwner().getId() == localPlayer.getId()) ||
                            (opts.booleanOption(OptionsConstants.ADVANCED_TEAM_VISION) &&
                                   (e.getOwner().getTeam() == localPlayer.getTeam())));
    }

    /**
     * Used to determine if this EntitySprite is only detected by an enemies sensors and hence should only be a sensor
     * return.
     *
     * @return
     */
    private boolean onlyDetectedBySensors() {
        boolean sensors = (bv.game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS) ||
                                 bv.game.getOptions()
                                       .booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS));
        boolean sensorsDetectAll = bv.game.getOptions().booleanOption(OptionsConstants.ADVANCED_SENSORS_DETECT_ALL);
        boolean doubleBlind = bv.game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        boolean hasVisual = entity.hasSeenEntity(bv.getLocalPlayer());
        boolean hasDetected = entity.hasDetectedEntity(bv.getLocalPlayer());

        return sensors &&
                     doubleBlind &&
                     !sensorsDetectAll &&
                     !trackThisEntitiesVisibilityInfo(entity) &&
                     hasDetected &&
                     !hasVisual;
    }

    @Override
    protected int getSpritePriority() {
        return entity.getSpriteDrawPriority() + 10;
    }
}
