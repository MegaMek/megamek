/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.game;

import megamek.common.Player;
import megamek.common.units.BTObject;

/**
 * This interface represents all objects that any type of BT Game (TW, Alpha Strike, BattleForce, SBF, ISAW) should
 * possibly keep in its list of playable "entities" or game units, such as Entity and AlphaStrikeElement and at some
 * point maybe BFUnits, SBFFormations or ACS Combat Teams, but also Objective Markers, carryable objects or buildings.
 * Each such InGameObject must have a game-unique id and can, but does not have to have an owning player.
 * <p>
 * The purpose of this interface is to disentangle Game, Client etc. from Entity and facilitate other types of games
 * than TW.
 */
public interface InGameObject extends BTObject {

    /**
     * Returns this InGameObject's id. The id must be unique to this InGameObject within the current game. equals() must
     * return true for two InGameObject objects with the same id.
     *
     * @return The game-unique id of this InGameObject (Entity, AlphaStrikeElement etc.)
     */
    int getId();

    /**
     * Sets this InGameObject's id. The id must be unique to this InGameObject within the current game. equals() must
     * return true for two InGameObject objects with the same id.
     *
     * @param newId The game-unique id of this InGameObject (Entity, AlphaStrikeElement etc.)
     */
    void setId(int newId);

    /**
     * Returns the unique id of this InGameObject's owning player. This id may be Player.NONE.
     *
     * @return The player id of the owner of this InGameObject.
     */
    int getOwnerId();

    /**
     * Sets the unique id of this InGameObject's owning player. This id may be Player.NONE.
     *
     * @param newOwnerId The player id of the owner of this InGameObject.
     */
    void setOwnerId(int newOwnerId);

    /**
     * Returns true when the owner id of this InGameObject is not Player.NONE.
     *
     * @return True when this InGameObject has an owning player
     */
    default boolean hasOwner() {
        return getOwnerId() != Player.PLAYER_NONE;
    }

    /**
     * Returns the current (remaining) battle strength of this unit or object. For combat units, this is the battle
     * value (BV) or the point value (PV).
     *
     * @return The current battle strength (BV/PV)
     */
    int getStrength();

    /**
     * Returns true when the current (remaining) battle strength of this unit/object should be counted for a strength
     * sum, e.g. if it should count for the summed battle value of a player or team. This may be false when the unit is
     * destroyed or trapped or otherwise permanently kept from acting or when it is part of a unit group and its
     * strength will be counted through that unit group (e.g. FighterSquadrons). See {@link #getStrength()}.
     *
     * @return True when the strength of this should be counted in a strength sum
     */
    default boolean countForStrengthSum() {
        return true;
    }
}
