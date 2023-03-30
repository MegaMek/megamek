/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

/**
 * This interface represents all objects that any type of BT Game (TW, Alpha Strike, BattleForce, SBF, ISAW)
 * should possibly keep in its list of playable "entities" or game units, such as Entity and AlphaStrikeElement
 * and at some point maybe BFUnits, SBFFormations or ACS Combat Teams, but also Objective Markers, carryable
 * objects or buildings. Each such InGameObject must have a game-unique id and can, but does not have to have
 * an owning player.
 *
 * The purpose of this interface is to disentangle Game, Client etc. from Entity and facilitate other
 * types of games than TW.
 */
public interface InGameObject extends BTObject {

    /**
     * Returns this InGameObject's id. The id must be unique to this InGameObject within the current game.
     * equals() must return true for two InGameObject objects with the same id.
     *
     * @return The game-unique id of this InGameObject (Entity, AlphaStrikeElement etc.)
     */
    int getId();

    /**
     * Returns the unique id of this InGameObject's owning player. This id may be Player.NONE.
     *
     * @return The player id of the owner of this InGameObject.
     */
    int getOwnerId();

    /**
     * Returns true when the owner id of this InGameObject is not Player.NONE.
     *
     * @return True when this InGameObject has an owning player
     */
    default boolean hasOwner() {
        return getOwnerId() != Player.PLAYER_NONE;
    }

    /**
     * Returns the current (remaining) battle strength of this unit or object. For combat units, this
     * is the battle value (BV) or the point value (PV).
     *
     * @return The current battle strength (BV/PV)
     */
    int getStrength();
}