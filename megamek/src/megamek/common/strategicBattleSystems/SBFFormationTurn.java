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

package megamek.common.strategicBattleSystems;

import megamek.common.game.IGame;
import megamek.common.game.InGameObject;

/**
 * This is a turn for a player action that uses a unit (formation). Examples are movement and firing.
 */
public class SBFFormationTurn extends SBFTurn {

    /**
     * Creates a new player turn for an SBF Game.
     *
     * @param playerId The player who has to take action
     */
    public SBFFormationTurn(int playerId) {
        super(playerId);
    }

    @Override
    public boolean isValid(SBFGame game) {
        return (game.getPlayer(playerId()) != null) && game.hasEligibleFormation(this);
    }

    @Override
    public boolean isValidEntity(InGameObject unit, IGame game) {
        return (unit.getOwnerId() == playerId()) && unit instanceof SBFFormation
              && ((SBFFormation) unit).isEligibleForPhase(game.getPhase());
    }
}
