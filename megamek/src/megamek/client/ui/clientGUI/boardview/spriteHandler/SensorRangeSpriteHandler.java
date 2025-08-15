/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.SensorRangeSprite;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

public class SensorRangeSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private final Game game;

    // The last-used values are cached so they're available when the sensor range view is turned on
    private Entity currentEntity;
    private Coords currentPosition;

    public SensorRangeSpriteHandler(AbstractClientGUI clientGUI, Game game) {
        super(clientGUI);
        this.game = game;
    }

    private boolean shouldShowSensorRange() {
        GamePhase phase = game.getPhase();
        return GUIP.getShowSensorRange() &&
              (phase.isDeployment() || phase.isMovement() || phase.isTargeting()
                    || phase.isFiring() || phase.isOffboard());
    }

    @Override
    public void clear() {
        super.clear();
        currentEntity = null;
        currentPosition = null;
    }

    @Override
    public void initialize() {
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        clear();
        GUIP.removePreferenceChangeListener(this);
    }

    public void setSensorRange(Entity entity, Coords assumedPosition) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        currentEntity = entity;
        currentPosition = assumedPosition;

        if ((entity == null) || entity.isOffBoard() || (assumedPosition == null) || !shouldShowSensorRange()) {
            return;
        }

        List<RangeHelper> lBrackets = new ArrayList<>(1);
        int minSensorRange = 0;
        int maxSensorRange = 0;
        int minAirSensorRange = 0;
        int maxAirSensorRange = 0;
        var gameOptions = game.getOptions();

        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
              || (gameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS))
              && entity.isSpaceborne()) {
            Compute.SensorRangeHelper srh = Compute.getSensorRanges(entity.getGame(), entity);

            if (srh != null) {
                if (entity.isAirborne() && game.isOnGroundMap(entity)) {
                    minSensorRange = srh.minGroundSensorRange;
                    maxSensorRange = srh.maxGroundSensorRange;
                    minAirSensorRange = srh.minSensorRange;
                    maxAirSensorRange = srh.maxSensorRange;
                } else {
                    minSensorRange = srh.minSensorRange;
                    maxSensorRange = srh.maxSensorRange;
                }
            }
        }

        lBrackets.add(new RangeHelper(minSensorRange, maxSensorRange));
        lBrackets.add(new RangeHelper(minAirSensorRange, maxAirSensorRange));

        minSensorRange = 0;
        maxSensorRange = Compute.getMaxVisualRange(entity, false);

        lBrackets.add(new RangeHelper(minSensorRange, maxSensorRange));

        if (game.getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            maxSensorRange = Compute.getMaxVisualRange(entity, true);
        } else {
            maxSensorRange = 0;
        }

        lBrackets.add(new RangeHelper(minSensorRange, maxSensorRange));

        // create the lists of hexes
        List<Set<Coords>> sensorRanges = new ArrayList<>(1);
        int j = 0;

        // find max range possible on map, no need to check beyond it
        Board board = game.getBoard(entity);
        int rangeToCorner = (new Coords(0, board.getHeight())).distance(assumedPosition);
        rangeToCorner = Math.max(rangeToCorner, (new Coords(0, 0)).distance(assumedPosition));
        rangeToCorner = Math.max(rangeToCorner, (new Coords(board.getWidth(), 0)).distance(assumedPosition));
        rangeToCorner = Math.max(rangeToCorner,
              (new Coords(board.getWidth(), board.getHeight())).distance(assumedPosition));

        for (RangeHelper rangeH : lBrackets) {
            sensorRanges.add(new HashSet<>());
            int rangeMin = Math.min(rangeH.min, rangeToCorner);
            int rangeMax = Math.min(rangeH.max, rangeToCorner);

            if (rangeMin != rangeMax) {
                for (int i = rangeMin; i <= rangeMax; i++) {
                    // Add all hexes up to the range to separate lists
                    sensorRanges.get(j).addAll(assumedPosition.allAtDistance(i));
                }
            }

            // Remove hexes that are not on the board
            sensorRanges.get(j).removeIf(h -> !board.contains(h));
            j++;
        }

        // create the sprites

        // for all available range
        BoardView boardView = (BoardView) clientGUI.getBoardView(board.getBoardId());
        for (int b = 0; b < lBrackets.size(); b++) {
            if (sensorRanges.get(b) == null) {
                continue;
            }

            for (Coords loc : sensorRanges.get(b)) {
                // check surrounding hexes
                int edgesToPaint = 0;

                for (int dir = 0; dir < 6; dir++) {
                    Coords adjacentHex = loc.translated(dir);

                    if (!sensorRanges.get(b).contains(adjacentHex)) {
                        edgesToPaint += (1 << dir);
                    }
                }

                // create sprite if there's a border to paint
                if (edgesToPaint > 0) {
                    currentSprites.add(new SensorRangeSprite(boardView, b, loc, edgesToPaint));
                }
            }
        }
        boardView.addSprites(currentSprites);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getName().equals(GUIPreferences.SHOW_SENSOR_RANGE)) {
            setSensorRange(currentEntity, currentPosition);
        }
    }

    private record RangeHelper(int min, int max) {}
}
