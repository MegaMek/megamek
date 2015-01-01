/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot;

import java.util.Comparator;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.WeaponType;

public class AttackOption extends ToHitData {

    /**
     * 
     */
    private static final long serialVersionUID = -8566472187475019360L;

    static class Sorter implements Comparator<AttackOption> {
        CEntity primary = null;

        public Sorter(CEntity primary_target) {
            primary = primary_target;
        }

        public int compare(AttackOption a, AttackOption a1) {
            if (a.target.getKey().intValue() == a1.target.getKey().intValue()) {
                WeaponType w = (WeaponType) a.weapon.getType();
                WeaponType w1 = (WeaponType) a1.weapon.getType();
                if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    if (w1.getDamage() == WeaponType.DAMAGE_MISSILE) {
                        if (a.expected > a1.expected) {
                            return -1;
                        }
                        return 1;
                    }
                    return 1;
                } else if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    return -1;
                } else if (a.expected > a1.expected) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (a.target.getKey().equals(primary.getKey())) {
                return -1;
            }
            return 1;
        }
    }

    public CEntity target;
    public double value;
    public Mounted weapon;
    public ToHitData toHit;
    public double odds; // secondary odds
    public double primary_odds; // primary odds
    public int heat;
    public double expected; // damage adjusted by secondary to-hit odds
    public double primary_expected; // damage adjusted by primary to-hit odds
    public int ammoLeft = -1; // -1 doesn't use ammo
    public String use_mode = "None"; // The mode the weapon is set to for

    // this option

    // TODO: Add argument for the precise bin of ammo being used for this option
    // so it can be reloaded later
    public AttackOption(CEntity target, Mounted weapon, double value,
            ToHitData toHit, int sec_mod) {
        this.target = target;
        this.weapon = weapon;
        this.toHit = toHit;
        this.value = value;

        if (target != null && weapon != null) {
            if (weapon.getType().getModesCount() > 0) {
                use_mode = weapon.curMode().getName();
            }
            WeaponType w = (WeaponType) weapon.getType();

            // As a primary attack. Damage is already odds-adjusted.
            primary_odds = Compute.oddsAbove(toHit.getValue()) / 100.0;
            primary_expected = this.value;

            // As a secondary attack. Raw damage is extracted, then adjusted
            // for secondary to-hit odds. Since units with active Stealth armor
            // cannot be secondary targets, chances of hitting are 0.

            if (target.getEntity().isStealthActive()) {
                odds = 0.0;
            } else {
                odds = sec_mod <= 12 ? (Compute.oddsAbove(toHit.getValue()
                        + sec_mod) / 100.0) : 0.0;
            }
            heat = w.getHeat();
            expected = this.value / primary_odds;
            expected = expected * odds;

            // Check for ammo; note that some conventional infantry and BA
            // weapons do NOT return AmmoType.T_NA

            final boolean isInfantryWeapon = w.hasFlag(WeaponType.F_INFANTRY);
            final boolean usesAmmo = (!isInfantryWeapon & w.getAmmoType() != AmmoType.T_NA);

            final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
            if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0)) {
                this.value = 0.0; // should have already been caught...
                primary_expected = 0.0;
                expected = 0.0;
            } else if (usesAmmo) {
                ammoLeft = ammo.getShotsLeft();
            }
        }
    }
}