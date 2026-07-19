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

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.rules.core.CoreRulesWeapons;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;

import java.util.Vector;

public class TWRulesWeapons extends CoreRulesWeapons {

    // RAC is restricted in what it can do in weapons phase
    @Override
    public boolean getRACUnjamRestriction() {
        return true;
    }

    // ATMs cluster in 5s
    @Override
    public int getATMClusterSize() { return 5; }

    // UACs jam
    @Override
    public boolean canUACsJam() { return true; }

    // ACs get hit normally
    @Override
    public void setACHit(CriticalSlot cs, Mounted<?> mounted, Vector<Report> reports, int entityId) {}

    // ELRMs get half missiles hit under minimum
    @Override
    public int getELRMMinimumRackSize(int rackSize) { return (rackSize / 2 + rackSize % 2); }

    // MRMs are +1 to hit
    @Override
    public int getMRMModifier(int modifier) { return (modifier + 1); }

    // MRMs have no cluster modifier, but with Apollo they do
    @Override
    public int getMRMClusterModifier(boolean apollo) {
        if (apollo) {
            return -1;
        }
        return 0;
    }

    // Apollo is -1 to hit
    @Override
    public int getApolloToHit() { return -1; }

    @Override
    public boolean flamerHeatAndDamage(boolean bmmFlamers) {
        return bmmFlamers ? true : false;
    }

    @Override
    @Nullable
    public Report checkPPCCapacitor(int roll, Entity attackingEntity, WeaponMounted
          weapon) {
        Report r = new Report(3178);
        if (roll == 2) {
            r.subject = attackingEntity.getId();
            r.indent();
            // Oops, we ruined our day...
            int wLocation = weapon.getLocation();
            weapon.setHit(true);
            for (int i = 0; i < attackingEntity.getNumberOfCriticalSlots(wLocation); i++) {
                CriticalSlot slot = attackingEntity.getCritical(wLocation, i);
                if ((slot == null)
                      || (slot.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                // Only one Crit needs to be damaged.
                Mounted<?> mounted = slot.getMount();
                if (mounted.equals(weapon)) {
                    slot.setDestroyed(true);
                    break;
                }
            }
            return r;
        }
        return null;
    }

    // No MGA cluster bonus
    @Override
    public int getMGABonus() {
        return 0;
    }

    // HGR can cause PSR
    @Override
    public boolean canHGRTriggerPSR(int mpUsed, int weightClass) {
        if (mpUsed > 0 && weightClass <= EntityWeightClass.WEIGHT_ASSAULT) {
            return true;
        }
        return false;
    }
}
