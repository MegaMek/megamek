/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.sprite.isometric;

import java.awt.*;
import java.awt.image.ImageObserver;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.HexSprite;
import megamek.client.ui.util.EntityWreckHelper;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

/**
 * Sprite used for isometric rendering to render an entity partially hidden behind a hill.
 */
public class IsometricSprite extends HexSprite {

    Entity entity;
    private final Image radarBlipImage;
    private final int secondaryPos;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public IsometricSprite(BoardView boardView1, Entity entity, int secondaryPos, Image radarBlipImage) {
        super(boardView1, secondaryPos == -1 ? entity.getPosition() : entity.getSecondaryPositions().get(secondaryPos));
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        String shortName = entity.getShortName();
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        Rectangle modelRect = new Rectangle(47,
              55,
              bv.getPanel().getFontMetrics(font).stringWidth(shortName) + 1,
              bv.getPanel().getFontMetrics(font).getAscent());

        int altAdjust = 0;
        if (bv.useIsometric() && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            altAdjust = (int) (bv.DROP_SHADOW_DISTANCE * bv.getScale());
        } else if (bv.useIsometric() && (entity.getElevation() != 0) && !(entity.isBuildingEntityOrGunEmplacement())) {
            altAdjust = (int) (entity.getElevation() * BoardView.HEX_ELEV * bv.getScale());
        }

        Dimension dim = new Dimension(bv.getHexSize().width, bv.getHexSize().height + altAdjust);
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

    public Entity getEntity() {
        return entity;
    }

    @Override
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
            Image shadow = bv.createShadowMask(bv.getTileManager().imageFor(entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);
            // Draw airborne units in 2 passes. Shadow is rendered
            // during the opaque pass, and the
            // Actual unit is rendered during the transparent pass.
            // However, the unit is always drawn
            // opaque.
            if (makeTranslucent) {
                g.drawImage(image, p.x, p.y - (int) (bv.DROP_SHADOW_DISTANCE * bv.getScale()), this);
            } else {
                g.drawImage(shadow, p.x, p.y, this);
            }
        } else if ((entity.getElevation() != 0) && !(entity.isBuildingEntityOrGunEmplacement())) {
            Image shadow = bv.createShadowMask(bv.getTileManager().imageFor(entity, entity.getFacing(), secondaryPos));
            shadow = bv.getScaledImage(shadow, true);

            // Entities on a bridge hex or submerged in water.
            int altAdjust = (int) (entity.getElevation() * BoardView.HEX_ELEV * bv.getScale());
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
            Image fuelLeak = bv.getScaledImage(bv.getTileManager().bottomLayerFuelLeakMarkerFor(entity), true);
            if (null != fuelLeak) {
                graph.drawImage(fuelLeak, x, y, observer);
            }
        }

        // draw the 'tires' or 'tracks' decal where appropriate
        boolean drawMotiveWreckage = EntityWreckHelper.displayMotiveDamage(entity);

        if (drawMotiveWreckage) {
            Image motiveWreckage = bv.getScaledImage(bv.getTilesetManager().bottomLayerMotiveMarkerFor(entity), true);
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
        g.drawImage(bv.getScaledImage(bv.getTileManager().imageFor(entity, secondaryPos), true), 0, 0, this);
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
     */
    private boolean onlyDetectedBySensors() {
        boolean sensors = (bv.game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS) ||
              bv.game.getOptions()
                    .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS));
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
