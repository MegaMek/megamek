/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.minimap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import jakarta.annotation.Nullable;
import megamek.client.ui.clientGUI.DamageLevelColors;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;

/**
 * Draws the damage pip on a mini-map unit symbol: a small dot in the top-right corner, coloured by the unit's damage
 * level. The pip is drawn in the symbol's own coordinates (about 100 units across), so it zooms and stacks with the
 * symbol, but it never shrinks below {@link #MINIMUM_RADIUS_PIXELS} on screen.
 */
public final class MinimapDamagePip {

    /** Pip centre and radius on the Standard symbols, in symbol units. */
    static final double STANDARD_CENTER_X = 45;
    static final double STANDARD_CENTER_Y = -45;
    static final double STANDARD_RADIUS = 24;

    /** Pip centre and radius on the StratOps box (167 x 103 symbol units), in symbol units. */
    static final double STRAT_OPS_CENTER_X = 72;
    static final double STRAT_OPS_CENTER_Y = -44;
    static final double STRAT_OPS_RADIUS = 26;

    /**
     * Smallest pip radius on screen, in pixels. At zoom 0 and 1 the pip would otherwise be under 2 pixels. Like the
     * rest of the mini-map's sizes (HEX_SIDE, UNIT_SCALE), this is a fixed pixel value and is not GUI-scaled.
     */
    static final double MINIMUM_RADIUS_PIXELS = 2.0;

    /** Outline width in symbol units, and its smallest on-screen width in pixels. */
    static final double OUTLINE_WIDTH = 10;
    static final double MINIMUM_OUTLINE_PIXELS = 0.75;

    private MinimapDamagePip() {
    }

    /**
     * Returns true when the unit should get a damage pip. It gets one when the Show damage level option is on, it is
     * still in the game, and it is damaged. Under double-blind, only your own units get one, plus your teammates' units
     * when team vision is on; enemy damage stays hidden even when the enemy unit is in sight.
     *
     * @param entity          the unit being drawn
     * @param localPlayer     the player looking at the mini-map; may be {@code null}
     * @param showDamageLevel the value of the Show damage level option
     * @param removedFromGame {@code true} when the unit is drawn as a destroyed or removed unit
     *
     * @return {@code true} when the pip should be drawn
     */
    public static boolean shouldDraw(Entity entity, @Nullable Player localPlayer, boolean showDamageLevel,
          boolean removedFromGame) {
        if (!showDamageLevel || removedFromGame) {
            return false;
        }
        if (entity.getDamageLevel() == Entity.DMG_NONE) {
            return false;
        }
        if (!isDoubleBlind(entity)) {
            return true;
        }
        return EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity);
    }

    private static boolean isDoubleBlind(Entity entity) {
        Game game = entity.getGame();
        return (game != null) && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
    }

    /**
     * Returns the pip radius in symbol units. This is the normal radius, made larger when needed so the pip is at
     * least {@link #MINIMUM_RADIUS_PIXELS} on screen.
     *
     * @param symbolScale the scale from symbol units to pixels (UNIT_SCALE[zoom] / 100)
     * @param stratOps    {@code true} for the StratOps symbols
     *
     * @return the radius in symbol units
     */
    static double radius(double symbolScale, boolean stratOps) {
        double normalRadius = stratOps ? STRAT_OPS_RADIUS : STANDARD_RADIUS;
        return Math.max(normalRadius, MINIMUM_RADIUS_PIXELS / symbolScale);
    }

    /**
     * Draws the pip for the unit, if it is damaged. Call it while the graphics transform is set to the unit symbol's
     * position and scale, and before any rotation for the facing arrow.
     *
     * @param graphics    the graphics, transformed to symbol units
     * @param entity      the unit being drawn
     * @param symbolScale the scale from symbol units to pixels (UNIT_SCALE[zoom] / 100)
     * @param stratOps    {@code true} for the StratOps symbols
     */
    public static void draw(Graphics2D graphics, Entity entity, double symbolScale, boolean stratOps) {
        Color fillColor = DamageLevelColors.forEntity(entity);
        if (fillColor == null) {
            return;
        }
        double radius = radius(symbolScale, stratOps);
        double centerX = stratOps ? STRAT_OPS_CENTER_X : STANDARD_CENTER_X;
        double centerY = stratOps ? STRAT_OPS_CENTER_Y : STANDARD_CENTER_Y;
        var pip = new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        float outlineWidth = (float) Math.max(OUTLINE_WIDTH, MINIMUM_OUTLINE_PIXELS / symbolScale);
        // A crippled pip is black, so it gets a white ring to stand out from the black symbol outline
        Color outlineColor = (entity.getDamageLevel() == Entity.DMG_CRIPPLED) ? Color.WHITE : Color.BLACK;

        Paint savedPaint = graphics.getPaint();
        Stroke savedStroke = graphics.getStroke();
        graphics.setColor(fillColor);
        graphics.fill(pip);
        graphics.setColor(outlineColor);
        graphics.setStroke(new BasicStroke(outlineWidth));
        graphics.draw(pip);
        graphics.setPaint(savedPaint);
        graphics.setStroke(savedStroke);
    }
}
