/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.AeroCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

import java.text.NumberFormat;
import java.util.*;

/**
 * AeroSpaceFighter subclass of Aero that encapsulates Fighter functionality
 */
public class AeroSpaceFighter extends Aero {
    public AeroSpaceFighter() {
        super();
    }

    @Override
    public int getUnitType() {
        return UnitType.AEROSPACEFIGHTER;
    }

    public void autoSetMaxBombPoints() {
        // Aerospace fighters can carry both external and internal ordnances, if configured and quirked
        // appropriately
        maxExtBombPoints = (int) Math.round(getWeight() / 5);
        // Can't check quirk here, as they don't exist in unit files yet.
        maxIntBombPoints = getTransportBays().stream().mapToInt(
                tb -> (tb instanceof CargoBay) ? (int) Math.floor(tb.getUnused()) : 0
        ).sum();
    }

    @Override
    public boolean isInASquadron() {
        return game.getEntity(getTransportId()) instanceof FighterSquadron;
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        return Math.max(0, t - (int) Math.ceil(getExternalBombPoints() / 5.0));
    }

    @Override
    public boolean isSpheroid() {
        return false;
    }

    // Damage a fighter that was part of a squadron when splitting it. Per
    // StratOps pg. 32 & 34
    @Override
    public void doDisbandDamage() {

        int dealt = 0;

        // Check for critical threshold and if so damage all armor on one facing
        // of the fighter completely,
        // reduce SI by half, and mark three engine hits.
        if (isDestroyed() || isDoomed()) {
            int loc = Compute.randomInt(4);
            dealt = getArmor(loc);
            setArmor(0, loc);
            int finalSI = Math.min(getSI(), getSI() / 2);
            dealt += getSI() - finalSI;
            setSI(finalSI);
            setEngineHits(Math.max(3, getEngineHits()));
        }

        // Move on to actual damage...
        int damage = getCap0Armor() - getCapArmor();
        // Fix for #587. Only multiply if Aero Sanity is off
        if ((null != game) && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage *= 10;
        }
        damage -= dealt; // We already dealt a bunch of damage, move on.
        if (damage < 1) {
            return;
        }
        int hits = (int) Math.ceil(damage / 5.0);
        int damPerHit = 5;
        for (int i = 0; i < hits; i++) {
            int loc = Compute.randomInt(4);
            // Fix for #587. Apply in 5 point groups unless damage remainder is less.
            setArmor(getArmor(loc) - Math.min(damPerHit, damage), loc);
            // We did too much damage, so we need to damage the SI, but we wont
            // reduce the SI below 1 here
            // unless the fighter is destroyed.
            if (getArmor(loc) < 0) {
                if (getSI() > 1) {
                    int si = getSI() + (getArmor(loc) / 2);
                    si = Math.max(si, isDestroyed() || isDoomed() ? 0 : 1);
                    setSI(si);
                }
                setArmor(0, loc);
            }
            damage -= damPerHit;
        }
    }

    /**
     * Damage a capital fighter's weapons. WeaponGroups are damaged by critical hits.
     * This matches up the individual fighter's weapons and critical slots and damages those
     * for MHQ resolution
     * @param loc - Int corresponding to the location struck
     */
    public void damageCapFighterWeapons(int loc) {
        for (Mounted weapon : weaponList) {
            if (weapon.getLocation() == loc) {
                //Damage the weapon
                weapon.setHit(true);
                //Damage the critical slot
                for (int i = 0; i < getNumberOfCriticals(loc); i++) {
                    CriticalSlot slot1 = getCritical(loc, i);
                    if ((slot1 == null) ||
                            (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }
                    Mounted mounted = slot1.getMount();
                    if (mounted.equals(weapon)) {
                        hitAllCriticals(loc, i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isAero() {
        return true;
    }

    /**
     * Fighters may carry external ordnance;
     * Other Aerospace units with cargo bays and the Internal Bomb Bay quirk may carry bombs internally.
     * @return boolean
     */
    @Override
    public boolean isBomber() {
        return true;
    }

    @Override
    /**
     * Returns true if this is an aerospace or conventional fighter
     * but not a larger craft (i.e. "SmallCraft" or "Dropship" and bigger
     */
    public boolean isFighter() {
        return true;
    }

    @Override
    /**
     * Returns true if and only if this is an aerospace fighter.
     */
    public boolean isAerospaceFighter() {
        return true;
    }

    @Override
    public long getEntityType() {
        return super.getEntityType() | Entity.ETYPE_AEROSPACEFIGHTER;
    }
}