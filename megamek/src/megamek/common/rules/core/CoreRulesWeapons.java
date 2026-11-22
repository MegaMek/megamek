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

import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rules.RulesWeapons;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class CoreRulesWeapons extends RulesWeapons {

    /**
     * {@inheritDoc}
     * RAC unjamming does not limit other actions outside of movement Core p.183
     */
    @Override
    public boolean getRACUnjamRestriction() {
        return false;
    }

    /**
     * {@inheritDoc}
     * ATM cluster size Core p.186
     */
    @Override
    public int getATMClusterSize() {
        return 6;
    }

    /**
     * {@inheritDoc}
     * Core writes the ultra autocannon, of every kind, without a jam (Core p.183).
     */
    @Override
    public boolean canUACsJam() {
        return false;
    }

    /**
     * {@inheritDoc}
     * The first critical hit "applied to an autocannon of any class or variety" does not destroy it, which Core p.183
     * applies to the standard, LB-X, light, ProtoMek, rotary and ultra autocannons it lists. Capital-scale naval
     * autocannons are not in Core and keep the Total Warfare critical hit.
     */
    @Override
    public void setACHit(CriticalSlot cs,
                         Mounted<?> mounted,
                         Vector<Report> reports,
                         int entityId) {
        if (!(mounted.getType() instanceof WeaponType weaponType)
              || !weaponType.hasFlag(WeaponType.F_AC)
              || weaponType.isCapital()) {
            return;
        }
        if (!mounted.isAutocannonHit()) {
            // A superheavy Mek can hold a two-slot autocannon in one slot, which then needs "two hits to the same
            // slot" (Core pp.183, 240)
            // A unit without critical slots, such as a building, passes none
            if ((cs != null)
                  && (!(mounted.getEntity() instanceof Mek) || (mounted.getEntity().occupiedCriticalSlots(mounted) == 1))) {
                cs.setHit(false);
            }
            mounted.setHit(false);
            mounted.setAutocannonHit(true);

            Report r = new Report(6256);
            r.subject = entityId;
            r.indent(2);
            r.add(mounted.getName());
            reports.addElement(r);
        }
    }

    /**
     * {@inheritDoc}
     * ELRMS under minimum do not reduce missiles that hit. Core p.186
     */
    @Override
    public int getELRMMinimumRackSize(int rackSize) {
        return rackSize;
    }

    /**
     * {@inheritDoc}
     * MRMs have no additional modifier Core p.186
     */
    @Override
    public int getMRMModifier(int modifier) {
        return modifier;
    }

    /**
     * {@inheritDoc}
     * MRMs are -1 on the cluster hit Core p.186. With apollo they are 0. Core p.197
     */
    @Override
    public int getMRMClusterModifier(boolean apollo) {
        if (apollo) {
            return 0;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     * Apollo does not change the to-hit Core p.197
     */
    @Override
    public int getApolloToHit() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Flamers do heat and damage Core p.183
     */
    @Override
    public boolean flamerHeatAndDamage(boolean bmmFlamers) {
        return true;
    }

    /**
     * {@inheritDoc}
     * PPC Capacitors don't break on roll. Core p.188
     */
    @Nullable
    @Override
    public Report checkPPCCapacitor(int roll,
                                    Entity attackingEntity,
                                    WeaponMounted
                                            weapon) {
        return null;
    }

    /**
     * {@inheritDoc}
     * MGA gives +2 to cluster roll. Core p.185
     */
    @Override
    public int getMGABonus() {
        return 2;
    }

    /**
     * {@inheritDoc}
     * Firing a heavy Gauss rifle does not cause a PSR (Core p.184)
     */
    @Override
    public boolean canHGRTriggerPSR(int mpUsed,
                                    int weightClass) {
        return false;
    }

    /**
     * {@inheritDoc}
     * The Apollo FCS has a saturation mode (Core p.197)
     */
    @Override
    public boolean getApolloSaturationMode() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Core writes the Thunderbolt launcher without halving damage under minimum range (Core p.187)
     */
    @Override
    public double thunderboltMinimum(final double toReturn,
                                     final int nRange,
                                     final int minRange,
                                     final boolean hotLoaded) {
        return toReturn;
    }
}
