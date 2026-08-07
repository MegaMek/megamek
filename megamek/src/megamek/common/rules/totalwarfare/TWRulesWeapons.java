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
import megamek.common.rules.RulesWeapons;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;

import java.util.Vector;

public class TWRulesWeapons extends RulesWeapons {

    /**
     * Does a RAC unjamming cause issues?
     * Yes, it is restricted in what it can do in the weapons phase
     *
     * @return true if RAC unjamming has restrictions
     */
    @Override
    public boolean getRACUnjamRestriction() {
        return true;
    }

    /**
     * What size do ATMs cluster in? 5s
     *
     * @return the ATM cluster size
     */
    @Override
    public int getATMClusterSize() { return 5; }

    /**
     * Can ultra autocannons jam? Yes.
     *
     * @return true if ultra autocannons can jam
     */
    @Override
    public boolean canUACsJam() { return true; }

    /**
     * What happens when an AC is hit? nothing extra
     * This function does nothing
     *
     * @param cs the critical slot being hit
     * @param mounted the mounted weapon
     * @param reports vector of reports describing the hit
     * @param entityId the ID of the entity being hit
     */
    @Override
    public void setACHit(CriticalSlot cs, Mounted<?> mounted, Vector<Report> reports, int entityId) {}

    /**
     * Extended LRMs halve the rack size under minimum
     *
     * @param rackSize the rack size
     * @return the minimum ELRM rack size
     */
    @Override
    public int getELRMMinimumRackSize(int rackSize) { return (rackSize / 2 + rackSize % 2); }

    /**
     * MRMs are +1 to hit
     *
     * @param modifier the base modifier
     * @return the MRM modifier
     */
    @Override
    public int getMRMModifier(int modifier) { return (modifier + 1); }

    /**
     * What is the cluster modifier for MRMs?
     * MRMs have no cluster modifier, but with Apollo they do
     * 
     * @param apollo true if Apollo fire control is used
     * @return the MRM cluster modifier
     */
    @Override
    public int getMRMClusterModifier(boolean apollo) {
        if (apollo) {
            return -1;
        }
        return 0;
    }

    /**
     * Apollos is -1 to hit
     *
     * @return the Apollo to-hit modifier
     */
    @Override
    public int getApolloToHit() { return -1; }

    /**
     * Do flamers do damage and heat?
     * Only if BMM Flamers is enabled
     *
     * @param bmmFlamers true if using alternate flamer rules
     * @return true if flamers do both damage and heat
     */
    @Override
    public boolean flamerHeatAndDamage(boolean bmmFlamers) {
        return bmmFlamers ? true : false;
    }

    /**
     * PPC Capacitor check.
     *
     * @param roll the dice roll result
     * @param attackingEntity the entity firing the PPC
     * @param weapon the PPC weapon being fired
     * @return a report of the capacitor check result
     */
    @Nullable
    @Override
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

    /**
     * No MGA cluster bonus
     *
     * @return the MGA bonus
     */
    @Override
    public int getMGABonus() {
        return 0;
    }

    /**
     * Does the HGR trigger a PSR? Only if they moved and it isn't an assault mek
     *
     * @param mpUsed the movement points used
     * @param weightClass the weight class of the unit
     * @return true if HGR can trigger a piloting skill roll
     */
    @Override
    public boolean canHGRTriggerPSR(int mpUsed, int weightClass) {
        if (mpUsed > 0 && weightClass <= EntityWeightClass.WEIGHT_ASSAULT) {
            return true;
        }
        return false;
    }
}
