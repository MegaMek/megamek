package megamek.common.rules.totalwarfare;

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


import megamek.common.rules.core.CoreRulesMovement;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;

public class TWRulesMovement extends CoreRulesMovement {

    // TW has skidding
    @Override
    public boolean skidEnabled() {
        return true;
    }

    // Can you run / flank in water? Vehicles and mechs cannot
    @Override
    public boolean cannnotRunInWater(EntityMovementMode movementMode,
          boolean amphibious) {
        if  ((movementMode != EntityMovementMode.HOVER) &&
              (movementMode!= EntityMovementMode.NAVAL) &&
              (movementMode != EntityMovementMode.HYDROFOIL) &&
              (movementMode != EntityMovementMode.INF_UMU) &&
              (movementMode != EntityMovementMode.SUBMARINE) &&
              (movementMode != EntityMovementMode.VTOL) &&
              (movementMode != EntityMovementMode.WIGE) &&
              !amphibious) {
            return true;
        }
        return false;
    }

    // Fully underwater hexes cost 3MP
    public int getUnderwaterMPCost() {
        return 3;
    }
    
    // Backwards elevation changes only if tacops rule
    @Override
    public boolean enableBackwardsElevationChange(final boolean toBackwardsElevation, Entity entity) {
        return toBackwardsElevation;
    }

    // Do we add leg damage together, no unless TO.
    @Override
    public boolean cumulativeLegDamage(boolean bTOLegDamage) {
        return bTOLegDamage;
    }
    
    // Only unconscious, shut down, or leg destruction causes immobile
    @Override
    public boolean checkMPZeroCauseImmobile(int walkMP) { return false; }
    
    @Override
    public int getMekRunMP(int badLegs, int walkMP, int runMP) {
        if (badLegs == 0) {
            return runMP;
        } else {
            return walkMP;
        }
    }

    // Moving into water always triggers PSR danger
    @Override
    public boolean isMoveIntoWaterDangerous(EntityMovementType movementType, EntityMovementMode movementMode) {
        return true;
    }
}
