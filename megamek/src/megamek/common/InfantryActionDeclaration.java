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
package megamek.common;

import java.io.Serializable;
import java.util.List;

/**
 * One player's declaration for an infantry vs. infantry action in one building, made in the Pre-End Declarations
 * phase (TO:AR pp. 169 to 172). An attacker commits units, or withdraws the force; a defender commits units and a
 * number of crew. It travels in a packet and is applied to unit state; it is never saved itself.
 *
 * @param playerId         the declaring player
 * @param buildingId       the building the action is in, or would be in
 * @param committedUnitIds the player's infantry units, by id, committed this turn; empty for none
 * @param committedCrew    crew the defending player commits this turn, on top of any already committed; {@code 0}
 *                         for an attacker
 * @param withdraw         {@code true} when the player withdraws their whole force this turn; a defender may only
 *                         under the house rule that allows it
 */
public record InfantryActionDeclaration(int playerId, int buildingId, List<Integer> committedUnitIds,
      int committedCrew, boolean withdraw) implements Serializable {

    /**
     * An attacker's declaration.
     *
     * @param playerId         the attacking player
     * @param buildingId       the building
     * @param committedUnitIds the units committed
     * @param withdraw         whether the force withdraws
     *
     * @return the declaration
     */
    public static InfantryActionDeclaration attacking(int playerId, int buildingId, List<Integer> committedUnitIds,
          boolean withdraw) {
        return new InfantryActionDeclaration(playerId, buildingId, List.copyOf(committedUnitIds), 0, withdraw);
    }

    /**
     * A defender's declaration.
     *
     * @param playerId         the defending player
     * @param buildingId       the building
     * @param committedUnitIds the infantry committed
     * @param committedCrew    the crew committed this turn
     *
     * @return the declaration
     */
    public static InfantryActionDeclaration defending(int playerId, int buildingId, List<Integer> committedUnitIds,
          int committedCrew) {
        return defending(playerId, buildingId, committedUnitIds, committedCrew, false);
    }

    /**
     * A defender's declaration that may withdraw the defending infantry, under the house rule that allows it.
     *
     * @param playerId         the defending player
     * @param buildingId       the building
     * @param committedUnitIds the infantry committed; ignored when withdrawing
     * @param committedCrew    the crew committed this turn; ignored when withdrawing
     * @param withdraw         {@code true} to withdraw every defending infantry unit of the player this turn
     *
     * @return the declaration
     */
    public static InfantryActionDeclaration defending(int playerId, int buildingId, List<Integer> committedUnitIds,
          int committedCrew, boolean withdraw) {
        return new InfantryActionDeclaration(playerId, buildingId, withdraw ? List.of() : List.copyOf(committedUnitIds),
              withdraw ? 0 : committedCrew, withdraw);
    }
}
