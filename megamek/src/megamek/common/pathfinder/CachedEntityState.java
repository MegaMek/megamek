/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.pathfinder;

import java.util.HashMap;
import java.util.Map;

import megamek.common.MPCalculationSetting;
import megamek.common.equipment.EquipmentFlag;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

/**
 * A transient class used to lazy-load "calculated" information from an entity
 */
public class CachedEntityState {
    private final Entity backingEntity;

    private Integer walkMP;
    private Integer runMP;
    private Integer runMPWithOneMasc;
    private Integer runMPWithoutMasc;
    private Integer runMPNoGravity;
    private Integer sprintMP;
    private Integer sprintMPWithOneMasc;
    private Integer sprintMPWithoutMasc;
    private Integer jumpMP;
    private Integer jumpMPWithTerrain;
    private final Map<EquipmentFlag, Boolean> hasWorkingMisc;
    private Integer torsoJumpJets;
    private Integer jumpMPNoGravity;
    private Integer numBreachedLegs;

    public CachedEntityState(Entity entity) {
        backingEntity = entity;
        hasWorkingMisc = new HashMap<>();
    }

    public int getWalkMP() {
        if (walkMP == null) {
            walkMP = backingEntity.getWalkMP();
        }

        return walkMP;
    }

    public int getRunMP() {
        if (runMP == null) {
            runMP = backingEntity.getRunMP();
        }

        return runMP;
    }

    public int getRunMPWithoutMASC() {
        if (runMPWithoutMasc == null) {
            runMPWithoutMasc = backingEntity.getRunMPWithoutMASC();
        }

        return runMPWithoutMasc;
    }

    public int getRunMPWithOneMASC() {
        if (runMPWithOneMasc == null) {
            runMPWithOneMasc = backingEntity.getRunMP(MPCalculationSetting.ONE_MASC);
        }

        return runMPWithOneMasc;
    }

    public int getSprintMP() {
        if (sprintMP == null) {
            sprintMP = backingEntity.getSprintMP();
        }

        return sprintMP;
    }

    public int getSprintMPWithOneMASC() {
        if (sprintMPWithOneMasc == null) {
            sprintMPWithOneMasc = backingEntity.getSprintMPWithOneMASC();
        }

        return sprintMPWithOneMasc;
    }

    public int getSprintMPWithoutMASC() {
        if (sprintMPWithoutMasc == null) {
            sprintMPWithoutMasc = backingEntity.getSprintMPWithoutMASC();
        }

        return sprintMPWithoutMasc;
    }

    public int getJumpMP() {
        if (jumpMP == null) {
            jumpMP = backingEntity.getAnyTypeMaxJumpMP();
        }

        return jumpMP;
    }

    public int getJumpMPWithTerrain() {
        if (jumpMPWithTerrain == null) {
            jumpMPWithTerrain = backingEntity.getJumpMPWithTerrain();
        }

        return jumpMPWithTerrain;
    }

    public boolean hasWorkingMisc(EquipmentFlag flag) {
        if (!hasWorkingMisc.containsKey(flag)) {
            hasWorkingMisc.put(flag, backingEntity.hasWorkingMisc(flag));
        }

        return hasWorkingMisc.get(flag);
    }

    public int getTorsoJumpJets() {
        if (torsoJumpJets == null) {
            if (backingEntity instanceof Mek) {
                torsoJumpJets = ((Mek) backingEntity).torsoJumpJets();
            } else {
                torsoJumpJets = 0;
            }
        }

        return torsoJumpJets;
    }

    public int getJumpMPNoGravity() {
        if (jumpMPNoGravity == null) {
            jumpMPNoGravity = backingEntity.getJumpMP(MPCalculationSetting.NO_GRAVITY);
        }

        return jumpMPNoGravity;
    }

    public int getRunMPNoGravity() {
        if (runMPNoGravity == null) {
            runMPNoGravity = backingEntity.getRunningGravityLimit();
        }

        return runMPNoGravity;
    }

    /**
     * Convenience property to determine if the backing entity is amphibious.
     */
    public boolean isAmphibious() {
        return hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
              hasWorkingMisc(MiscType.F_AMPHIBIOUS) ||
              hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
    }

    /**
     * Convenience property to determine how many armor-breached legs this entity has. By default, this is 0 unless the
     * unit is a mek.
     */
    public int getNumBreachedLegs() {
        if (numBreachedLegs == null) {
            if (backingEntity instanceof QuadMek) {
                numBreachedLegs = ((backingEntity.getArmor(QuadMek.LOC_LEFT_LEG) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(QuadMek.LOC_LEFT_ARM) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(QuadMek.LOC_RIGHT_LEG) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(QuadMek.LOC_RIGHT_ARM) > 0) ? 0 : 1);
            } else if (backingEntity instanceof TripodMek) {
                numBreachedLegs = ((backingEntity.getArmor(TripodMek.LOC_LEFT_LEG) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(TripodMek.LOC_CENTER_LEG) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(TripodMek.LOC_RIGHT_LEG) > 0) ? 0 : 1);
            } else if (backingEntity instanceof Mek) {
                numBreachedLegs = ((backingEntity.getArmor(Mek.LOC_LEFT_LEG) > 0) ? 0 : 1) +
                      ((backingEntity.getArmor(Mek.LOC_RIGHT_LEG) > 0) ? 0 : 1);
            } else {
                numBreachedLegs = 0;
            }
        }

        return numBreachedLegs;
    }
}
