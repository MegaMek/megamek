package megamek.common.rules.core;
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


import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.rules.RulesGame;

public class CoreRulesGame extends RulesGame {

    /**
     * {@inheritDoc}
     * Ammo dumping is not in Core
     */
    public boolean ammoDumping() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Immobile not eligible in movement Core p.49
     * RAC Unjamming does not prevent usage (only limits movement) Core p.183
     * Finding a club can use in physical phase Core p.79
     */
    public boolean eligibleForPhase(boolean unjammingRAC, boolean findingClub, boolean immobile,
          @Nullable GamePhase phase) {
        if (phase != null) {
            if (immobile && phase.isMovement()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * Front loaded initiative Core p.41
     */
    public int getInitiativeOrder(int[] num_turns, int index, int min, boolean frontLoadOption) {
        return ((int) Math.ceil(((double) num_turns[index]) / (double) min));
    }
    
    /**
     * {@inheritDoc}
     * No BV boost for semi-guided or Arrow IV homing (Not present in core)
     */
    public boolean tagBVBump() {
        return false;
    }
}
