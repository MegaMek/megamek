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

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.enums.GamePhase;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SensorRangeSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {
    
    private final Game game;

    // The last-used values are cached so they're available when the sensor range view is turned on
    private Entity currentEntity;
    private Coords currentPosition;
    
    public SensorRangeSpriteHandler(BoardView boardView, Game game) {
        super(boardView);
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
        GameOptions gameOptions = game.getOptions();

        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || (gameOptions.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) && entity.isSpaceborne()) {
            Compute.SensorRangeHelper srh = Compute.getSensorRanges(entity.getGame(), entity);

            if (srh != null) {
                if (entity.isAirborne() && entity.getGame().getBoard().onGround()) {
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
        int rangeToCorner = (new Coords(0, game.getBoard().getHeight())).distance(assumedPosition);
        rangeToCorner = Math.max(rangeToCorner, (new Coords(0, 0)).distance(assumedPosition));
        rangeToCorner = Math.max(rangeToCorner, (new Coords(game.getBoard().getWidth(), 0)).distance(assumedPosition));
        rangeToCorner = Math.max(rangeToCorner, (new Coords(game.getBoard().getWidth(), game.getBoard().getHeight())).distance(assumedPosition));

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
            sensorRanges.get(j).removeIf(h -> !game.getBoard().contains(h));
            j++;
        }

        // create the sprites

        // for all available range
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

    private static class RangeHelper {
        public int min;
        public int max;

        private RangeHelper(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }
}
