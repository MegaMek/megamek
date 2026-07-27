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


import megamek.common.MPCalculationSetting;
import megamek.common.equipment.MiscType;
import megamek.common.rules.RulesMovement;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

import java.util.ArrayList;

public class CoreRulesMovement extends RulesMovement {
    // No skidding in Core Rules
    public boolean skidEnabled() {
        return false;
    }

    // Can you run / flank in water? Core p.51
    public boolean cannotRunInWater(EntityMovementMode movementMode,
          boolean amphibious) {
        if ((movementMode != EntityMovementMode.HOVER) &&
              (movementMode != EntityMovementMode.NAVAL) &&
              (movementMode != EntityMovementMode.HYDROFOIL) &&
              (movementMode != EntityMovementMode.INF_UMU) &&
              (movementMode != EntityMovementMode.SUBMARINE) &&
              (movementMode != EntityMovementMode.VTOL) &&
              (movementMode != EntityMovementMode.WIGE) &&
              (movementMode != EntityMovementMode.BIPED) &&
              (movementMode != EntityMovementMode.QUAD) &&
              (movementMode != EntityMovementMode.TRIPOD) &&
              !amphibious) {
            return true;
        }
        return false;
    }

    // Fully underwater hexes cost 2MP. Core p.51
    public int getUnderwaterMPCost() {
        return 2;
    }

    // Backwards elevation changes are enabled. Core p.46
    public boolean enableBackwardsElevationChange(final boolean toBackwardsElevation, Entity entity) {
        if (entity instanceof Mek) {
            int legsDestroyed = ((Mek) entity).countBadLegs();
            if (legsDestroyed >= 1 && !(entity instanceof QuadMek)) {
                // No backwards elevation with a bad leg
                return false;
            } else if (legsDestroyed >= 3) {
                // Quads run into this with 3 legs gone
                return false;
            }
            return true;
        }
        // Not a Mek
        return false;
    }

    // Do we add leg damage together, yes. Core p.98
    public boolean cumulativeLegDamage(boolean bTOLegDamage) {
        return true;
    }

    // Run is still allowed with a broken leg. Core p.90
    public int getMekRunMP(int badLegs, int walkMP, int runMP) {
        return runMP;
    }
    
    // meks can only change 1 elevation level when leg destroyed. Core p.90, p.238
    public boolean reduceMaxElevation(Mek mek) {
        if (mek.atLeastOneBadLeg()) {
            if (mek instanceof QuadMek) { 
                 if (mek.countBadLegs() >= 3) {
                    return true;
                 } else {
                     return false;
                 }
            } 
            return false;
        }
        return false;
    }
}
