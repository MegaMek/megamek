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
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.CraneOperation;
import megamek.common.units.CraneRules;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;

/**
 * Shows crane loading or unloading in progress at a grounded Small Craft or DropShip (TW p.90-91). The hex carries a
 * countdown of the turns of crane work still to go. When unloading, the unit also fades in on the hex it will be placed
 * in, the same way a bridge under construction fades in, and is fully visible on the turn it comes out.
 */
public class CraneOperationSprite extends HexSprite {

    private static final Color TEXT_OUTLINE_COLOR = new Color(40, 40, 50);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int HEX_CENTER_Y = HexTileset.HEX_H / 2;
    private static final int FONT_SIZE = 28;

    /** Minimum opacity of a fading unit, so it stays faintly visible on the first and last turns of crane work. */
    private static final float FADE_MIN_ALPHA = 0.18f;

    /** The unit fading in on the hex, or null when only the countdown is shown. */
    private final Entity fadingUnit;
    private final int facing;
    private final int turnsCompleted;
    private final int turnsRequired;

    /**
     * Creates a crane work sprite.
     *
     * @param boardView      the parent board view
     * @param location       the hex the unit waits in (loading) or will be placed in (unloading)
     * @param fadingUnit     the unit to fade in on the hex, or {@code null} to show only the countdown
     * @param facing         the facing the fading unit is drawn with
     * @param turnsCompleted the turns of crane work banked so far
     * @param turnsRequired  the turns of crane work the operation needs
     */
    public CraneOperationSprite(BoardView boardView, Coords location, @Nullable Entity fadingUnit, int facing,
          int turnsCompleted, int turnsRequired) {
        super(boardView, location);
        this.fadingUnit = fadingUnit;
        this.facing = facing;
        this.turnsCompleted = turnsCompleted;
        this.turnsRequired = turnsRequired;
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        if (fadingUnit != null) {
            drawFadingUnit(graph, fadingUnit);
        }
        drawCountdown(graph);
        graph.dispose();
    }

    /**
     * Draws the unit being unloaded at an opacity equal to the crane work done: nothing banked shows a faint outline,
     * the last turn shows it fully.
     *
     * @param graph the sprite graphics
     * @param unit  the unit being unloaded
     */
    private void drawFadingUnit(Graphics2D graph, Entity unit) {
        Image unitImage = bv.getTilesetManager().imageFor(unit, facing, -1);
        if (unitImage == null) {
            return;
        }
        // A sprite paints its buffer once, so wait for the image to finish loading; ImageIcon blocks until it has
        Image loadedImage = new ImageIcon(unitImage).getImage();
        float alpha = fadeOpacity(turnsCompleted, turnsRequired);

        Composite oldComposite = graph.getComposite();
        graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graph.drawImage(loadedImage, 0, 0, null);
        graph.setComposite(oldComposite);
    }

    /**
     * Draws the number of turns of crane work still to go in the middle of the hex.
     *
     * @param graph the sprite graphics
     */
    private void drawCountdown(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String countdown = Integer.toString(Math.max(0, turnsRequired - turnsCompleted));
        Font countdownFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, FONT_SIZE);
        FontMetrics metrics = graph.getFontMetrics(countdownFont);
        int textX = HEX_CENTER_X - (metrics.stringWidth(countdown) / 2);
        int textY = HEX_CENTER_Y + (metrics.getAscent() / 2) - 2;

        // White text with a dark outline so it reads on any terrain and over the unit
        graph.setFont(countdownFont);
        graph.setColor(TEXT_OUTLINE_COLOR);
        for (int offsetX = -2; offsetX <= 2; offsetX += 2) {
            for (int offsetY = -2; offsetY <= 2; offsetY += 2) {
                graph.drawString(countdown, textX + offsetX, textY + offsetY);
            }
        }
        graph.setColor(Color.WHITE);
        graph.drawString(countdown, textX, textY);
    }

    /**
     * Returns how opaque to draw a unit the cranes are loading (TW p.90): fully visible when the work starts, fading as
     * each turn is banked, so it is faint on its last turn before it goes aboard.
     *
     * @param unit the unit on the board
     * @param game the game, used to find a carrier working on the unit
     *
     * @return the opacity to draw the unit with; 1 for a unit no crane is loading
     */
    public static float loadingOpacity(Entity unit, Game game) {
        if (!CraneRules.isCraneOnlyUnit(unit)) {
            return 1f;
        }
        SmallCraft carrier = CraneRules.findCarrierWorkingOn(unit.getId(), game);
        CraneOperation operation = (carrier == null) ? null : carrier.getCraneOperations().findFor(unit.getId());
        if ((operation == null) || !operation.isLoading() || !isWaitingInPlace(unit, operation)) {
            return 1f;
        }
        return fadeOpacity(operation.getTurnsRequired() - operation.getTurnsCompleted(),
              operation.getTurnsRequired());
    }

    /**
     * Checks whether a unit waiting to be loaded is still where it declared from and has not moved this turn. A unit that
     * moves loses its place (the End Phase cancels the loading and reports it), so its countdown and fade are removed
     * as soon as the move shows on the board.
     *
     * @param unit      the unit waiting to be loaded
     * @param operation its crane loading operation
     *
     * @return {@code true} if the unit is still waiting in place for the cranes
     */
    public static boolean isWaitingInPlace(Entity unit, CraneOperation operation) {
        return operation.getUnitPosition().equals(unit.getPosition()) && (unit.delta_distance == 0);
    }

    /**
     * Converts crane work into an opacity. It never drops below a faint minimum, so a unit being moved by the cranes
     * never disappears from the board.
     *
     * @param turnsVisible  the turns of the unit's picture to show: banked turns when fading in, turns left when
     *                      fading out
     * @param turnsRequired the turns of crane work the operation needs
     *
     * @return the opacity, from the faint minimum up to 1
     */
    static float fadeOpacity(int turnsVisible, int turnsRequired) {
        float fraction = Math.clamp((float) turnsVisible / Math.max(1, turnsRequired), 0f, 1f);
        return Math.max(FADE_MIN_ALPHA, fraction);
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
