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

package megamek.common.units;

import java.io.Serial;

import megamek.common.game.Game;
import megamek.common.game.GameTurn;

/**
 * A type of game turn that allows only entities belonging to certain units to move.
 */
public class UnitNumberTurn extends GameTurn {
    @Serial
    private static final long serialVersionUID = -681892308327846884L;
    private final short unitNumber;

    /**
     * Only allow entities for the given player which have types in the class mask to move.
     *
     * @param playerId the <code>int</code> ID of the player
     * @param unit     the <code>int</code> unit number of the entities allowed to move.
     */
    public UnitNumberTurn(int playerId, short unit) {
        super(playerId);
        unitNumber = unit;
    }

    /**
     * @return true if the specified entity is a valid one to use for this turn.
     */
    @Override
    public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
        return super.isValidEntity(entity, game, useValidNonInfantryCheck)
              && (unitNumber == entity.getUnitNumber());
    }
}
