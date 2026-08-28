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

package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.WeaponType;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.TaintedAtmosphereRules;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests what a flammable atmosphere does to a weapon attack on conventional infantry, TO:AR p.54.
 * <p>
 * The rules predicates say which atmospheres change an attack; these tests check the damage that actually comes out
 * of the Non-Infantry Weapon Damage Against Infantry Table (TW p.217) once they have. Flammable tainted air shifts an
 * attack two rows down that table, and flammable toxic air takes the attack off the table altogether and applies its
 * damage point for point, as though another infantry unit had fired it.
 */
class FlammableAtmosphereInfantryDamageTest {

    private static final int NO_MARGIN_OF_SUCCESS = 0;
    private static final int SINGLE_WEAPON = 1;

    /**
     * Reads a weapon's damage off the infantry table the way a handler does, with no direct blow and no reports.
     *
     * @param damage     the weapon's damage value
     * @param damageType the row of the table to read
     * @param shift      how many rows a direct blow shifts it down the table first
     *
     * @return the number of troopers hit
     */
    private int troopersHit(int damage, int damageType, int shift) {
        return Compute.directBlowInfantryDamage(damage, shift, damageType, false, false,
              Entity.NONE, null, SINGLE_WEAPON);
    }

    /**
     * The row a flammable tainted atmosphere moves an attack onto, applying the two-rows-better shift and its
     * area-effect cap the same way {@code WeaponHandler.resolveInfantryDamageClass} does.
     *
     * @param baseDamageClass the row the weapon would use in breathable air
     *
     * @return the row its damage should be read off instead
     */
    private int shiftedByFlammableTaintedAir(int baseDamageClass) {
        int rowsBetter = TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.FLAMMABLE_TAINTED);
        int shiftedClass = baseDamageClass + rowsBetter;
        return (shiftedClass > WeaponType.WEAPON_CLUSTER_MISSILE)
              ? WeaponType.WEAPON_AREA_EFFECT_INFANTRY
              : shiftedClass;
    }

    @Test
    @DisplayName("In breathable air a direct-fire weapon kills a tenth of its damage in troopers")
    void directFireIsUnchangedInBreathableAir() {
        assertEquals(0, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.BREATHABLE),
              "breathable air shifts nothing");

        assertEquals(5, troopersHit(50, WeaponType.WEAPON_DIRECT_FIRE, NO_MARGIN_OF_SUCCESS),
              "direct fire is damage / 10");
    }

    @Test
    @DisplayName("Flammable tainted air shifts direct fire two rows, to the pulse row")
    void flammableTaintedAirShiftsDirectFireToPulse() {
        assertEquals(2, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.FLAMMABLE_TAINTED),
              "flammable tainted air shifts two rows");

        int inBreathableAir = troopersHit(50, WeaponType.WEAPON_DIRECT_FIRE, NO_MARGIN_OF_SUCCESS);
        int inFlammableAir = troopersHit(50, shiftedByFlammableTaintedAir(WeaponType.WEAPON_DIRECT_FIRE),
              NO_MARGIN_OF_SUCCESS);
        int onThePulseRow = troopersHit(50, WeaponType.WEAPON_PULSE, NO_MARGIN_OF_SUCCESS);

        assertEquals(onThePulseRow, inFlammableAir,
              "direct fire two rows down the table is the pulse row: damage / 10 + 2");
        assertTrue(inFlammableAir > inBreathableAir, "the burning air must not make the attack gentler");
    }

    @Test
    @DisplayName("Flammable tainted air shifts cluster ballistic two rows, to the cluster missile row")
    void flammableTaintedAirShiftsClusterBallisticToClusterMissile() {
        int inFlammableAir = troopersHit(50, shiftedByFlammableTaintedAir(WeaponType.WEAPON_CLUSTER_BALLISTIC),
              NO_MARGIN_OF_SUCCESS);
        int onTheClusterMissileRow = troopersHit(50, WeaponType.WEAPON_CLUSTER_MISSILE, NO_MARGIN_OF_SUCCESS);

        assertEquals(onTheClusterMissileRow, inFlammableAir,
              "cluster ballistic two rows down the table is the cluster missile row: damage / 5");
    }

    @Test
    @DisplayName("The book's own example: a cluster missile attack in flammable tainted air becomes area-effect")
    void clusterMissileIsCappedAtAreaEffect() {
        // TO:AR p.54: "a Cluster (Missile) attack would be considered an area-effect attack for purposes of
        // assigning damage". Two rows better than cluster missile runs off the end of the table, so it caps there.
        int damageValue = 50;

        int onTheClusterMissileRow = troopersHit(damageValue, WeaponType.WEAPON_CLUSTER_MISSILE,
              NO_MARGIN_OF_SUCCESS);
        int onTheAreaEffectRow = troopersHit(damageValue,
              shiftedByFlammableTaintedAir(WeaponType.WEAPON_CLUSTER_MISSILE), NO_MARGIN_OF_SUCCESS);

        assertEquals(WeaponType.WEAPON_AREA_EFFECT_INFANTRY,
              shiftedByFlammableTaintedAir(WeaponType.WEAPON_CLUSTER_MISSILE),
              "cluster missile shifted two rows caps at area-effect");

        assertEquals(damageValue / 5, onTheClusterMissileRow, "cluster missile is damage / 5");
        assertEquals(damageValue * 2, onTheAreaEffectRow, "area-effect is damage / .5, which is damage doubled");
        assertTrue(onTheAreaEffectRow > onTheClusterMissileRow,
              "the cap must be the harshest row on the table, not a step back down it");
    }

    @Test
    @DisplayName("Area-effect damage cannot be shifted further by a direct blow")
    void areaEffectIsAlreadyTheWorstRow() {
        int withoutDirectBlow = troopersHit(30, WeaponType.WEAPON_AREA_EFFECT_INFANTRY, NO_MARGIN_OF_SUCCESS);
        int withDirectBlow = troopersHit(30, WeaponType.WEAPON_AREA_EFFECT_INFANTRY, 2);

        assertEquals(withoutDirectBlow, withDirectBlow, "there is no row past area-effect to shift onto");
        assertEquals(60, withDirectBlow, "and it is still damage doubled");
    }

    @Test
    @DisplayName("A direct blow and a flammable atmosphere shift the same attack together")
    void aDirectBlowAndTheAtmosphereBothShift() {
        int atmosphereShift = TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.FLAMMABLE_TAINTED);
        int directBlowShift = 1;

        int shiftedByBoth = troopersHit(50, WeaponType.WEAPON_DIRECT_FIRE, directBlowShift + atmosphereShift);
        int shiftedByTheAtmosphereAlone = troopersHit(50, WeaponType.WEAPON_DIRECT_FIRE, atmosphereShift);

        assertTrue(shiftedByBoth > shiftedByTheAtmosphereAlone,
              "a direct blow in burning air lands harder than either on its own");
    }

    @Test
    @DisplayName("Flammable toxic air applies an attack's damage point for point, skipping the table")
    void flammableToxicAirAppliesDamageDirectly() {
        assertTrue(TaintedAtmosphereRules.treatsAttacksOnInfantryAsInfantryDamage(AtmosphericTaint.FLAMMABLE_TOXIC),
              "flammable toxic air resolves attacks as though infantry had made them");

        int damageValue = 50;
        int troopersHit = troopersHit(damageValue, WeaponType.WEAPON_INFANTRY_ORIGIN, NO_MARGIN_OF_SUCCESS);

        assertEquals(damageValue, troopersHit,
              "damage from another infantry unit is applied point for point, not read off the table");
    }

    @Test
    @DisplayName("Infantry-origin damage is far worse for a platoon than the same weapon on the table")
    void infantryOriginDamageBeatsTheTable() {
        int onTheTable = troopersHit(50, WeaponType.WEAPON_DIRECT_FIRE, NO_MARGIN_OF_SUCCESS);
        int appliedDirectly = troopersHit(50, WeaponType.WEAPON_INFANTRY_ORIGIN, NO_MARGIN_OF_SUCCESS);

        assertTrue(appliedDirectly > onTheTable,
              "skipping the table is what makes flammable toxic air so lethal to infantry");
    }

    @Test
    @DisplayName("A margin of success cannot shift an attack off the infantry-origin path")
    void infantryOriginDamageIgnoresTheMarginOfSuccess() {
        int withoutDirectBlow = troopersHit(30, WeaponType.WEAPON_INFANTRY_ORIGIN, NO_MARGIN_OF_SUCCESS);
        int withDirectBlow = troopersHit(30, WeaponType.WEAPON_INFANTRY_ORIGIN, 2);

        assertEquals(withoutDirectBlow, withDirectBlow,
              "the attack is off the table, so there is no row for a direct blow to shift it along");
        assertEquals(30, withDirectBlow, "and its damage is still applied point for point");
    }
}
