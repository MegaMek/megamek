package megamek.common.rules.totalwarfare;
/*
 * Copyright (C) 2026 James Magnan (bmazur@sev.org)
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.rules.core.CoreRulesGame;

public class TWRulesGame extends CoreRulesGame {

    // Ammo dumping is allowed
    @Override
    public boolean ammoDumping() { return true; }

    // In RW, unjamming the RAC or finding a club makes you ineligible for the phase
    @Override
    public boolean eligibleForPhase(boolean unjammingRAC, boolean findingClub, boolean immobile,
          @Nullable GamePhase phase) {
        if (unjammingRAC || findingClub) {
            return false;
        }
        return true;
    }

    // Only do front-loaded init if the option is selected
    @Override
    public int getInitiativeOrder(int[] num_turns, int index, int min, boolean frontLoadOption) {
        return frontLoadOption ? super.getInitiativeOrder(num_turns, index, min, true) :
              (num_turns[index] / min);
    }
    
    // TAG can increase BV when Semi-guided or homing arrow IV is present
    public boolean tagBVBump() {
        return true;
    }
}
