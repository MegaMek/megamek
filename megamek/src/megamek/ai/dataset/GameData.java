/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
 */
package megamek.ai.dataset;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Minefield;

/**
 * Container for game state data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class GameData extends EntityDataMap<GameData.Field> {

    /**
     * Enum defining general game state fields.
     */
    public enum Field {
        ROUND,
        PHASE,
        TURN_INDEX,
        TURN_PLAYER_ID,
        ENTITIES,    // Special field that contains a list of UnitState objects
        MINEFIELDS    // Special field that contains a list of minefield data
    }

    /**
     * Simple class to represent minefield data
     */
    public static class MinefieldData {
        private final int x;
        private final int y;
        private final int type;
        private final int playerId;
        private final int damage;

        public MinefieldData(Minefield minefield) {
            this.x = minefield.getCoords().getX();
            this.y = minefield.getCoords().getY();
            this.type = minefield.getType();
            this.playerId = minefield.getPlayerId();
            this.damage = minefield.getDensity();
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getType() {
            return type;
        }

        public int getPlayerId() {
            return playerId;
        }

        public int getDamage() {
            return damage;
        }
    }

    /**
     * Creates an empty GameData.
     */
    public GameData() {
        super(Field.class);
    }

    /**
     * Creates a GameData from a Game.
     * @param game The game to extract data from
     * @return A populated GameData
     */
    public static GameData fromGame(Game game) {
        GameData data = new GameData();

        // Basic game information
        data.put(Field.ROUND, game.getCurrentRound())
              .put(Field.PHASE, game.getPhase())
              .put(Field.TURN_INDEX, game.getTurnIndex())
              .put(Field.TURN_PLAYER_ID, game.getTurn() != null ? game.getTurn().playerId() : -1);

        // Extract entity data
        List<UnitState> entities = new ArrayList<>();
        for (var inGameObject : game.getInGameObjects()) {
            if (inGameObject instanceof Entity entity) {
                entities.add(UnitState.fromEntity(entity, game));
            }
        }
        data.put(Field.ENTITIES, entities);

        // Extract minefield data
        List<MinefieldData> minefields = new ArrayList<>();
        Enumeration<Coords> minefieldCoords = game.getMinedCoords();
        while (minefieldCoords.hasMoreElements()) {
            Coords coords = minefieldCoords.nextElement();
            for (Minefield mf : game.getMinefields(coords)) {
                minefields.add(new MinefieldData(mf));
            }
        }
        data.put(Field.MINEFIELDS, minefields);

        return data;
    }

    /**
     * Gets the list of unit states in this game data.
     * @return List of UnitStateMap objects
     */
    @SuppressWarnings("unchecked")
    public List<UnitState> getUnitStates() {
        return (List<UnitState>) get(Field.ENTITIES);
    }

    /**
     * Gets the list of minefields in this game data.
     * @return List of MinefieldData objects
     */
    @SuppressWarnings("unchecked")
    public List<MinefieldData> getMinefields() {
        return (List<MinefieldData>) get(Field.MINEFIELDS);
    }
}
