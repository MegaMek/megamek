package megamek.common.rules;
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
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;

import java.util.Vector;

public abstract class RulesWeapons {
    /**
     * Does a RAC unjamming cause issues?
     *
     * @return true if RAC unjamming has restrictions
     */
    public abstract boolean getRACUnjamRestriction();

    /**
     * What size do ATMs cluster in.
     *
     * @return the ATM cluster size
     */
    public abstract int getATMClusterSize();

    /**
     * Can ultra autocannons jam?
     *
     * @return true if ultra autocannons can jam
     */
    public abstract boolean canUACsJam();

    /**
     * What happens when an AC is hit.
     *
     * @param cs the critical slot being hit
     * @param mounted the mounted weapon
     * @param reports vector of reports describing the hit
     * @param entityId the ID of the entity being hit
     */
    public abstract void setACHit(CriticalSlot cs, Mounted<?> mounted, Vector<Report> reports, int entityId);

    /**
     * Do ELRMs reduce rack size under minimums?
     *
     * @param rackSize the rack size
     * @return the minimum ELRM rack size
     */
    public abstract int getELRMMinimumRackSize(int rackSize);

    /**
     * What is the modifier on MRMs.
     *
     * @param modifier the base modifier
     * @return the MRM modifier
     */
    public abstract int getMRMModifier(int modifier);

    /**
     * What is the cluster modifier for MRMs.
     *
     * @param apollo true if Apollo fire control is used
     * @return the MRM cluster modifier
     */
    public abstract int getMRMClusterModifier(boolean apollo);

    /**
     * Does Apollo change to-hit number.
     *
     * @return the Apollo to-hit modifier
     */
    public abstract int getApolloToHit();

    /**
     * Do flamers do damage and heat.
     *
     * @param bmmFlamers true if using alternate flamer rules
     * @return true if flamers do both damage and heat
     */
    public abstract boolean flamerHeatAndDamage(boolean bmmFlamers);

    /**
     * PPC Capacitor check.
     *
     * @param roll the dice roll result
     * @param attackingEntity the entity firing the PPC
     * @param weapon the PPC weapon being fired
     * @return a report of the capacitor check result
     */
    public abstract Report checkPPCCapacitor(int roll, Entity attackingEntity, WeaponMounted weapon);

    /**
     * Increase the number of hits for MGA.
     *
     * @return the MGA bonus
     */
    public abstract int getMGABonus();

    /**
     * Does the HGR trigger a PSR?
     *
     * @param mpUsed the movement points used
     * @param weightClass the weight class of the unit
     * @return true if HGR can trigger a piloting skill roll
     */
    public abstract boolean canHGRTriggerPSR(int mpUsed, int weightClass);

    /**
     * Does Apollo have saturation mode?
     * @return default of false. This is only in core and newer.
     */
    public boolean getApolloSaturationMode() {
        return false;
    }
}
